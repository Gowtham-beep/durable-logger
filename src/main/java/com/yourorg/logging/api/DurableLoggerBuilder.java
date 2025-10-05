package com.yourorg.logging.api;

import com.yourorg.logging.DurableLoggerFactory;
import com.yourorg.logging.config.LoggerConfig;
import com.yourorg.logging.errors.DefaultErrorHandler;
import com.yourorg.logging.errors.ErrorHandler;
import com.yourorg.logging.storage.FileStorageAdapter;
import com.yourorg.logging.storage.StorageAdapter;
import com.yourorg.logging.core.DurableLogger;

import java.io.File;

public class DurableLoggerBuilder {
    private final LoggerConfig cfg = new LoggerConfig();
    private ErrorHandler errorHandler = new DefaultErrorHandler();

    public static DurableLoggerBuilder create() {
        return new DurableLoggerBuilder();
    }

    public DurableLoggerBuilder service(String name) {
        cfg.serviceName = name;
        return this;
    }

    public DurableLoggerBuilder level(String level) {
        cfg.level = level;
        return this;
    }

    public DurableLoggerBuilder queueCapacity(int cap) {
        cfg.queueCapacity = cap;
        return this;
    }

    public DurableLoggerBuilder maxBatchSize(int size) {
        cfg.maxBatchSize = size;
        return this;
    }

    public DurableLoggerBuilder maxBatchMillis(long millis) {
        cfg.maxBatchMillis = millis;
        return this;
    }

    public DurableLoggerBuilder fsync(boolean fsync) {
        cfg.fsyncOnWalAppend = fsync;
        return this;
    }

    public DurableLoggerBuilder wal(String path, String checkpoint) {
        cfg.wal = new LoggerConfig.Wal();
        cfg.wal.path = path;
        cfg.wal.checkpoint = checkpoint;
        return this;
    }

    public DurableLoggerBuilder fileStorage(String path) {
        cfg.storage = new LoggerConfig.Storage();
        cfg.storage.type = "file";
        cfg.storage.file = new LoggerConfig.Storage.FileStorage();
        cfg.storage.file.path = path;
        return this;
    }

    public DurableLoggerBuilder retention(double rotateSizeMB, int maxDays) {
        cfg.retention = new LoggerConfig.Retention();
        cfg.retention.rotateSizeMB = rotateSizeMB;
        cfg.retention.maxDays = maxDays;
        return this;
    }

    public DurableLoggerBuilder errorHandler(ErrorHandler handler) {
        this.errorHandler = handler;
        return this;
    }

    public DurableLogger build() throws Exception {
        cfg.validate();

        // build adapter
        StorageAdapter adapter = switch (cfg.storage.type) {
            case "file" -> new FileStorageAdapter(
                    new File(cfg.storage.file.path),
                    cfg.retention,
                    errorHandler
            );
            default -> throw new IllegalArgumentException("Unsupported storage type: " + cfg.storage.type);
        };

        DurableLogger durable = new DurableLogger(
                adapter,
                new File(cfg.wal.path),
                new File(cfg.wal.checkpoint),
                cfg.queueCapacity,
                cfg.fsyncOnWalAppend,
                cfg.maxBatchSize,
                cfg.maxBatchMillis
        );

        LogManager.init(adapter, durable);
        return durable;
    }
}
