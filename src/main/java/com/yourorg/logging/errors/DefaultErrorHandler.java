package com.yourorg.logging.errors;
import com.yourorg.logging.api.LogEntry;

public class DefaultErrorHandler implements ErrorHandler{
    @Override
    public void onWriteFailure(Exception e, LogEntry entry) {
        System.err.println("[durable-logger] write failure: " + e.getMessage());
        e.printStackTrace(System.err);
    }

    @Override
    public void onQueryFailure(Exception e, String rawLine) {
        System.err.println("[durable-logger] query parse failure: " + e.getMessage());
        // avoid printing rawLine if it may contain sensitive data in production
    }

    @Override
    public void onInternalError(Exception e, String context) {
        System.err.println("[durable-logger] internal error (" + context + "): " + e.getMessage());
        e.printStackTrace(System.err);
    }

}
