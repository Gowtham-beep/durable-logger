package com.yourorg.logging.errors;
import  com.yourorg.logging.api.LogEntry;
public interface ErrorHandler {
    void onWriteFailure(Exception e,LogEntry entry);

    void onQueryFailure(Exception e, String rawLine);

    void onInternalError(Exception e, String context);

}
