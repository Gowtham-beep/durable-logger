package com.yourorg.logging.core;

import com.yourorg.logging.api.LogLevel;

import java.time.Instant;
import java.util.Optional;

public class QueryRequest {
    private final Instant from;
    private final Instant to;
    private final Optional<LogLevel> level;
    private final String text;
    private final int limit;

    public QueryRequest(Instant from, Instant to, Optional<LogLevel> level, String text, int limit) {
        this.from = from; this.to = to; this.level = level; this.text = text; this.limit = limit;
    }

    public Instant getFrom(){ return from; }
    public Instant getTo(){ return to; }
    public Optional<LogLevel> getLevel(){ return level; }
    public String getText(){ return text; }
    public int getLimit(){ return limit; }
}
