package com.yourorg.logging;

import com.yourorg.logging.api.LogManager;
import com.yourorg.logging.api.Logger;
import com.yourorg.logging.config.LoggerConfig;
import com.yourorg.logging.config.LoggerConfigLoader;
import com.yourorg.logging.core.DurableLogger;
import com.yourorg.logging.storage.FileStorageAdapter;

import java.io.File;

public class DurableLoggerFactory {

    private static DurableLogger durable;

    public static void init(String configPath) throws Exception{
        LoggerConfig cfg = LoggerConfigLoader.load(configPath);

        //pick storage adapter
        var adapter = switch (cfg.storage.type){
            case "file"-> new FileStorageAdapter(new File(cfg.storage.file.path));
            // case "postgres" -> new PostgresStorageAdapter(cfg.storage.postgres)
            // case "kafka" -> new KafkaStorageAdapter(cfg.storage.kafka)
            default -> throw new IllegalArgumentException("Unsupported storage type");
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
        LogManager.init(adapter,durable);
    }
    public static Logger getLogger(Class<?> cls){
        if(durable == null) {
            throw new IllegalStateException("Logger not initialized. Call init() first.");
        }
        return LogManager.get().getLogger(cls);
    }
}
