package com.yourorg.logging.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoggerConfig {
    public String serviceName;
    public String level;
    public int queueCapacity;
    public int maxBatchSize;
    public long maxBatchMillis;
    public boolean fsyncOnWalAppend;

    public Wal wal;
    public Storage storage;
    public Retention retention;

    public static class Wal {
        public String path;
        public String checkpoint;
    }

    public static class Storage {
        public String type;   // file, postgres, kafka, s3
        public FileStorage file;
        public PostgresStorage postgres;
        public KafkaStorage kafka;

        public static class FileStorage {
            public String path;
        }

        public static class PostgresStorage {
            public String url;
            public String user;
            public String password;
        }

        public static class KafkaStorage {
            public String bootstrap;
            public String topic;
        }
    }

    public static class Retention {
        public double rotateSizeMB;
        public int maxDays;
    }

    public void validate(){
        if (queueCapacity <= 0) throw new IllegalArgumentException("queueCapacity must be > 0");
        if (maxBatchSize <= 0) throw new IllegalArgumentException("maxBatchSize must be > 0");
        if (maxBatchMillis <= 0) throw new IllegalArgumentException("maxBatchMillis must be > 0");

        if (wal == null) throw new IllegalArgumentException("wal configuration missing");
        if (wal.path == null || wal.path.isBlank()) throw new IllegalArgumentException("wal.path must be set");
        if (wal.checkpoint == null || wal.checkpoint.isBlank()) throw new IllegalArgumentException("wal.checkpoint must be set");

        if (storage == null) throw new IllegalArgumentException("storage configuration missing");
        if (storage.type == null || storage.type.isBlank()) throw new IllegalArgumentException("storage.type must be set");

        if ("file".equalsIgnoreCase(storage.type)) {
            if (storage.file == null || storage.file.path == null || storage.file.path.isBlank()) {
                throw new IllegalArgumentException("storage.file.path must be set for file storage");
            }
        }
        // Additional validations for other adapters can be added here.
        if (retention != null) {
            if (retention.rotateSizeMB < 0) throw new IllegalArgumentException("retention.rotateSizeMB must be >= 0");
            if (retention.maxDays < 0) throw new IllegalArgumentException("retention.maxDays must be >= 0");
        }
    }
}
