package com.yourorg.logging.api;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class LogEntry{
    private final String uuid;
    private final Instant timestamp;
    private final LogLevel level;
    private final String service;
    private final String message;
    private final Map<String,Object> fields;
    private final String traceId;
    private final String stack;

    private LogEntry(Builder b) {
        this.uuid = b.uuid;
        this.timestamp = b.timestamp;
        this.level = b.level;
        this.service = b.service;
        this.message = b.message;
        this.fields = b.fields;
        this.traceId = b.traceId;
        this.stack = b.stack;
    }
    public static Builder builder(){return new Builder();}

    public static final class Builder{
        private String uuid = UUID.randomUUID().toString();
        private Instant timestamp = Instant.now();
        private LogLevel level = LogLevel.INFO;
        private String service = "default";
        private String message;
        private Map<String,Object> fields = Map.of();
        private String traceId;
        private String stack;

        public Builder uuid(String uuid){this.uuid=uuid;return this;}
        public Builder timeStamp(Instant ts){this.timestamp=ts;return this;}
        public Builder level(LogLevel l){this.level=l;return this;}
        public Builder service(String s){this.service=s;return this;}
        public Builder message(String m){this.message=m;return this;}
        public Builder fields(Map<String,Object> f){this.fields=f;return this;}
        public Builder traceId(String t){this.traceId=t;return  this;}
        public Builder stack(String s){this.stack=s;return this;}
        public LogEntry build(){
            Objects.requireNonNull(message,"message");
            return new LogEntry(this);
        }
    }
    public String getUuid(){ return uuid; }
    public Instant getTimestamp(){ return timestamp; }
    public LogLevel getLevel(){ return level; }
    public String getService(){ return service; }
    public String getMessage(){ return message; }
    public Map<String,Object> getFields(){ return fields; }
    public String getTraceId(){ return traceId; }
    public String getStack(){ return stack; }
}