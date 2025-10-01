package com.yourorg.logging;

import com.yourorg.logging.api.LogLevel;
import com.yourorg.logging.api.Logger;
import com.yourorg.logging.core.QueryRequest;
import com.yourorg.logging.core.QueryResult;

import java.time.Instant;
import java.util.Optional;

public class Main {
    public static void main(String[] args) throws Exception {
        DurableLoggerFactory.init(null);

        boolean queryOnly = args.length > 0 && "query".equalsIgnoreCase(args[0]);
        Logger logger = DurableLoggerFactory.getLogger(Main.class);

        if (!queryOnly) {
            logger.info("Application started");
            logger.warn("This is a warning");
            logger.log(LogLevel.ERROR, "An error occurred");

            try {
                int x = 1 / 0;
            } catch (Exception ex) {
                logger.error("Caught exception during divide by zero", ex);
            }

            Thread.sleep(1000);
            logger.info("Possibly-WAL-only log (kill now to test replay)");
        }

        // Query ERROR logs in last 5 minutes
        QueryRequest req = new QueryRequest(
                Instant.now().minusSeconds(300),
                Instant.now(),
                Optional.of(LogLevel.ERROR),
                null,
                200
        );

        QueryResult result = DurableLoggerFactory.durableQuery(req);
        System.out.println("Found " + result.getEntries().size() + " ERROR logs:");
        result.getEntries().forEach(e ->
                System.out.println(Instant.ofEpochMilli(e.getTimestamp()) +
                        " [" + e.getLevel() + "] " + e.getMessage())
        );

        DurableLoggerFactory.close();
    }
}
