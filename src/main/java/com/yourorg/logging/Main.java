package com.yourorg.logging;

import com.yourorg.logging.api.LogLevel;
import com.yourorg.logging.api.LogManager;
import com.yourorg.logging.api.Logger;
import com.yourorg.logging.core.DurableLogger;
import com.yourorg.logging.core.QueryRequest;
import com.yourorg.logging.core.QueryResult;
import com.yourorg.logging.storage.FileStorageAdapter;

import java.io.File;
import java.time.Instant;
import java.util.Optional;

public class Main {
    public static void main(String[] args) throws Exception {
        File wal = new File("data/wal.log");
        File checkpoint = new File("data/wal.check");
        File store = new File("data/store.log");

        var adapter = new FileStorageAdapter(store);
        var durable = new DurableLogger(adapter, wal, checkpoint,
                10000,   // queue capacity
                true,    // fsync on WAL append
                500,     // max batch size
                200);    // max batch millis
        LogManager.init(adapter, durable);

        // If run with "query" argument, skip writing new logs and only query.
        boolean queryOnly = args.length > 0 && "query".equalsIgnoreCase(args[0]);

        if (!queryOnly) {
            Logger logger = LogManager.get().getLogger(Main.class);

            // --- Write some logs ---
            logger.info("Application started");
            logger.log(LogLevel.WARN, "This is a warning");
            logger.log(LogLevel.ERROR, "An error occurred");

            try {
                int x = 1 / 0;
            } catch (Exception ex) {
                logger.error("Caught exception during divide by zero", ex);
            }

            // Give flusher time to persist some logs (tweak if needed)
            Thread.sleep(1000);

            // Add a log that may only be in WAL if you kill the app right after this
            logger.info("Possibly-WAL-only log (kill now to test replay)");
            System.out.println("Wrote logs. You can kill the process now to test replay, or wait 5s.");
            Thread.sleep(5000);
        }

        // --- Query logs: last 5 minutes, search for 'error' ---
        QueryRequest req = new QueryRequest(
                Instant.now().minusSeconds(300),
                Instant.now(),
                Optional.of(LogLevel.ERROR),  // level filter
                null,                         // no text filter
                200
        );


        QueryResult result = adapter.query(req);
        System.out.println("Query result: Found " + result.getEntries().size() + " logs containing 'ERROR'");

        result.getEntries().forEach(e ->
                System.out.println(
                        Instant.ofEpochMilli(e.getTimestamp()) + " [" + e.getLevel() + "] " + e.getMessage()
                )
        );

        // clean shutdown (if not killed)
        durable.close();
        System.out.println("Logger closed cleanly.");
    }
}
