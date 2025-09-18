package com.yourorg.logging.storage;

import com.yourorg.logging.api.LogEntry;
import com.yourorg.logging.core.QueryRequest;
import com.yourorg.logging.core.QueryResult;

import java.util.List;

public interface StorageAdapter {
    void start() throws Exception;
    void stop() throws Exception;
    /**
     * Append a batch of entries to the underlying store. Implementations should
     * be idempotent with respect to LogEntry.uuid.
     */
    void append(List<LogEntry> entries) throws Exception; /**
     * Optional: query. Throw UnsupportedOperationException if not supported.
     */
    QueryResult query(QueryRequest request) throws UnsupportedOperationException;
}