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
}
