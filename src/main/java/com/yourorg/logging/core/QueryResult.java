package com.yourorg.logging.core;

import com.yourorg.logging.api.LogEntry;

import java.util.List;

public class QueryResult {
    private final List<LogEntry> entries;
    private final long total;

    public QueryResult(List<LogEntry> entries, long total) { this.entries = entries; this.total = total; }
    public List<LogEntry> getEntries(){ return entries; }
    public long getTotal(){ return total; }
}
