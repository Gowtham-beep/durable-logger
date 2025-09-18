package com.yourorg.logging.core;

import com.yourorg.logging.api.LogEntry;
import com.yourorg.logging.api.LogLevel;
import com.yourorg.logging.api.Logger;
import com.yourorg.logging.storage.StorageAdapter;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class DurableLogger implements AutoCloseable {
    private final StorageAdapter adapter;
    private final WalWriter walWriter;
    private final Checkpoint checkpoint;
    private final BlockingQueue<LogEntry> queue;
    private final Thread flusherThread;
    private final long maxBatchMillis;
    private final int maxBatchSize;
    private volatile boolean running = true;


    public DurableLogger(StorageAdapter adapter, File walFile, File checkpointFile, int queueCapacity,
                         boolean fsyncOnWalAppend, int maxBatchSize, long maxBatchMillis) throws Exception {
        this.adapter = adapter;
        this.walWriter = new WalWriter(walFile, fsyncOnWalAppend);
        this.checkpoint = new Checkpoint(checkpointFile);
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.maxBatchSize = maxBatchSize;
        this.maxBatchMillis = maxBatchMillis;
        adapter.start();

        // replay on startup
        replayIfNeeded(walFile);

        this.flusherThread = new Thread(this::flusherLoop, "durable-logger-flusher");
        flusherThread.start();
    }
    public Logger forCalss(Class<?> cls){
        String svc = cls.getName();
        return new Logger() {
            @Override public void log(LogLevel level, String message) { log(level, message, Map.of()); }
            @Override public void log(LogLevel level, String message, Map<String, Object> fields) {
                LogEntry e = LogEntry.builder().service(svc).level(level).message(message).fields(fields).timeStamp(Instant.now()).build();
                try {
                    // WAL append (sync) -> enqueue
                    walWriter.append(e);
                    boolean offered = queue.offer(e);
                    if (!offered) {
                        // queue full: apply drop policy - drop oldest
                        queue.poll();
                        queue.offer(e);
                    }
                } catch (Exception ex) {
                    // failure in WAL append: degrade gracefully - print to stderr and continue
                    System.err.println("WAL append failed: " + ex.getMessage());
                }
            }
            @Override public void error(String message, Throwable t) {
                LogEntry e = LogEntry.builder().service(svc).level(LogLevel.ERROR).message(message).stack(t == null ? null : getStack(t)).timeStamp(Instant.now()).build();
                try {
                    walWriter.append(e);
                    queue.offer(e);
                } catch (Exception ex) { System.err.println("WAL append failed: " + ex.getMessage()); }
            }
            @Override public void info(String message) { log(LogLevel.INFO, message); }
            @Override public void warn(String message) { log(LogLevel.WARN, message); }
        };
    }
    private static String getStack(Throwable t){
        StringBuilder sb = new StringBuilder();
        sb.append(t.toString()).append("\n");
        for (StackTraceElement s: t.getStackTrace()) sb.append("\tat ").append(s.toString()).append("\n");
        return sb.toString();
    }private void flusherLoop(){
        List<LogEntry> batch = new ArrayList<>(maxBatchSize);
        while(running) {
            try {
                LogEntry first = queue.poll(maxBatchMillis, TimeUnit.MILLISECONDS);
                if(first == null) {
                    // timeout: if we have a batch collect/flush
                    if(batch.isEmpty()) continue;
                } else {
                    batch.add(first);
                    queue.drainTo(batch, maxBatchSize - batch.size());
                }
                if (!batch.isEmpty()) {
                    try {
                        adapter.append(batch);
                        // simple checkpoint: persist last uuid
                        checkpoint.write(batch.get(batch.size()-1).getUuid());
                        batch.clear();
                    } catch (Exception ex) {
                        // storage failure: backoff
                        System.err.println("Storage append failed: " + ex.getMessage());
                        Thread.sleep(1000); // backoff simple
                    }
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        // flush remaining before exit
        if (!batch.isEmpty()) {
            try { adapter.append(batch); checkpoint.write(batch.get(batch.size()-1).getUuid()); }
            catch (Exception ex) { System.err.println("Flush on shutdown failed: " + ex.getMessage()); }
        }
    }

    private void replayIfNeeded(File walFile) throws Exception {
        // read WAL and replay records that are not committed
        WalReader reader = new WalReader(walFile);
        var all = reader.readAll();
        String lastUuid = checkpoint.read();
        int startIdx = 0;
        if (lastUuid != null) {
            // skip entries up to lastUuid (exclusive)
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getUuid().equals(lastUuid)) { startIdx = i + 1; break; }
            }
        }
        if (startIdx < all.size()) {
            List<LogEntry> toReplay = all.subList(startIdx, all.size());
            // replay in batches
            int idx = 0;
            while(idx < toReplay.size()){
                int end = Math.min(idx + maxBatchSize, toReplay.size());
                List<LogEntry> batch = toReplay.subList(idx, end);
                adapter.append(batch);
                checkpoint.write(batch.get(batch.size()-1).getUuid());
                idx = end;
            }
        }
    }

    @Override
    public void close() throws Exception {
        running = false;
        flusherThread.interrupt();
        flusherThread.join(5000);
        walWriter.close();
        adapter.stop();
    }
}