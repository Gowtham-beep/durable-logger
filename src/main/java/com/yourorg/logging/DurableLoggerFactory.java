package com.yourorg.logging;

import com.yourorg.logging.api.LogManager;
import com.yourorg.logging.api.Logger;
import com.yourorg.logging.config.LoggerConfig;
import com.yourorg.logging.config.LoggerConfigLoader;
import com.yourorg.logging.core.DurableLogger;
import com.yourorg.logging.core.QueryRequest;
import com.yourorg.logging.core.QueryResult;
import com.yourorg.logging.errors.DefaultErrorHandler;
import com.yourorg.logging.errors.ErrorHandler;
import com.yourorg.logging.storage.FileStorageAdapter;
import com.yourorg.logging.storage.StorageAdapter;

import java.io.File;

public class DurableLoggerFactory {

    private static DurableLogger durable;
    private static StorageAdapter adapter;
    public static void initDefault() throws Exception {
        init("src/main/resources/logger.yml");
    }

    /**
     * Initialize logger using config file.
     * If configPath is null/empty, load logger.yml from classpath (resources).
     */
    public static void init(String configPath) throws Exception {
        LoggerConfig cfg = (configPath == null || configPath.isEmpty())
                ? LoggerConfigLoader.loadFromClasspath("logger.yml")
                : LoggerConfigLoader.load(configPath);


        cfg.validate();

        ErrorHandler errorHandler = new DefaultErrorHandler();

        // pick storage adapter
        adapter = switch (cfg.storage.type) {
            case "file" -> new FileStorageAdapter(new File(cfg.storage.file.path),cfg.retention,errorHandler);
            // case "postgres" -> new PostgresStorageAdapter(cfg.storage.postgres);
            // case "kafka" -> new KafkaStorageAdapter(cfg.storage.kafka);
            default -> throw new IllegalArgumentException("Unsupported storage type: " + cfg.storage.type);
        };

        durable = new DurableLogger(
                adapter,
                new File(cfg.wal.path),
                new File(cfg.wal.checkpoint),
                cfg.queueCapacity,
                cfg.fsyncOnWalAppend,
                cfg.maxBatchSize,
                cfg.maxBatchMillis
        );

        LogManager.init(adapter, durable);
    }

    /** Get logger for a class */
    public static Logger getLogger(Class<?> cls) {
        if (durable == null) {
            throw new IllegalStateException("Logger not initialized. Call init() first.");
        }
        return LogManager.get().getLogger(cls);
    }

    /** Query logs */
    public static QueryResult durableQuery(QueryRequest req) {
        if (adapter == null) {
            throw new IllegalStateException("Logger not initialized. Call init() first.");
        }
        return adapter.query(req);
    }

    /** Close logger gracefully */
    public static void close() throws Exception {
        if (durable != null) {
            durable.close();
            durable = null;
        }
    }
}
