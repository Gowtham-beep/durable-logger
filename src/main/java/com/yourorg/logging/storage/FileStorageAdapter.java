package com.yourorg.logging.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.logging.api.LogEntry;
import com.yourorg.logging.config.LoggerConfig;
import com.yourorg.logging.core.QueryRequest;
import com.yourorg.logging.core.QueryResult;
import com.yourorg.logging.errors.ErrorHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class FileStorageAdapter implements StorageAdapter {
    private final File file;
    private final ObjectMapper om = new ObjectMapper();
    private BufferedWriter writer;
    private final LoggerConfig.Retention retention;
    private final ErrorHandler errorHandler;
    private volatile boolean closed = false; // guard to prevent writes after stop

    public FileStorageAdapter(File file, LoggerConfig.Retention retention, ErrorHandler errorHandler) {
        this.file = file;
        this.retention = retention;
        this.errorHandler = (errorHandler == null) ? new com.yourorg.logging.errors.DefaultErrorHandler() : errorHandler;
    }

    @Override
    public synchronized void start() throws Exception {
        if (closed) throw new IllegalStateException("adapter already closed");
        file.getParentFile().mkdirs();
        writer = new BufferedWriter(new FileWriter(file, true));
    }

    @Override
    public synchronized void stop() throws Exception {
        if (closed) return;
        closed = true;
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }

    @Override
    public synchronized void append(List<LogEntry> entries) throws Exception {
        if (closed) throw new IllegalStateException("adapter is closed");
        try {
            for (LogEntry e : entries) {
                String json = om.writeValueAsString(e);
                writer.write(json);
                writer.newLine();
            }
            writer.flush();
        } catch (Exception ex) {
            // call error handler for every failing entry (best effort)
            try {
                for (LogEntry e : entries) {
                    errorHandler.onWriteFailure(ex, e);
                }
            } catch (Exception ignore) {}
            throw ex;
        }

        // rotation check (non-blocking small cost)
        try {
            if (retention != null && retention.rotateSizeMB > 0) {
                double sizeMB = Files.size(file.toPath()) / (1024.0 * 1024.0);
                if (sizeMB > retention.rotateSizeMB) {
                    rotate();
                }
            }
        } catch (Exception ex) {
            // rotation errors considered internal
            try { errorHandler.onInternalError(ex, "rotate"); } catch (Exception ignore) {}
        }
    }

    private void rotate() {
        try {
            writer.flush();
            writer.close();

            // Ensure unique filename using timestamp + counter fallback
            String rotatedName = file.getPath() + "." + System.currentTimeMillis();
            Path rotatedPath = Path.of(rotatedName);
            int counter = 1;
            while (Files.exists(rotatedPath)) {
                rotatedPath = Path.of(file.getPath() + "." + System.currentTimeMillis() + "_" + counter++);
            }

            Files.move(file.toPath(), rotatedPath);
            System.out.println("[durable-logger] Rotated log file to: " + rotatedPath);

            // Reopen a fresh file for writing
            writer = new BufferedWriter(new FileWriter(file, true));

        } catch (Exception ex) {
            System.err.println("[durable-logger] internal error (rotate): " + ex.getMessage());
            try {
                // Fallback: reopen file if writer was closed or null
                if (writer == null) {
                    writer = new BufferedWriter(new FileWriter(file, true));
                }
            } catch (Exception reopenEx) {
                System.err.println("[durable-logger] reopen failed: " + reopenEx.getMessage());
            }
        }
    }


    @Override
    public QueryResult query(QueryRequest request) {
        List<LogEntry> out = new ArrayList<>();
        try {
            File dir = file.getParentFile();
            String baseName = file.getName();

            File[] candidates = dir.listFiles((d, name) -> name.startsWith(baseName));
            if (candidates != null) {
                for (File f : candidates) {
                    try (Stream<String> lines = Files.lines(f.toPath())) {
                        lines.forEach(line -> {
                            try {
                                LogEntry e = om.readValue(line, LogEntry.class);
                                Instant ts = Instant.ofEpochMilli(e.getTimestamp());

                                // time filter
                                if (ts.isBefore(request.getFrom()) || ts.isAfter(request.getTo())) return;

                                // level filter
                                if (request.getLevel().isPresent() && e.getLevel() != request.getLevel().get()) return;

                                // text filter (case-insensitive)
                                if (request.getText() != null && !e.getMessage().toLowerCase().contains(request.getText().toLowerCase()))
                                    return;

                                out.add(e);
                            } catch (Exception ex) {
                                // let error handler know about bad lines
                                try { errorHandler.onQueryFailure(ex, line); } catch (Exception ignore) {}
                            }
                        });
                    }
                }
            }
        } catch (Exception ex) {
            try { errorHandler.onInternalError(ex, "query"); } catch (Exception ignore) {}
            throw new RuntimeException(ex);
        }

        // sort and apply limit
        out.sort(Comparator.comparingLong(LogEntry::getTimestamp));
        int max = Math.min(request.getLimit(), out.size());
        return new QueryResult(out.subList(0, max), out.size());
    }
}
