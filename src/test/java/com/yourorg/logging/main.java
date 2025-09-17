package com.yourorg.durablelogger.sample;

import com.yourorg.durablelogger.api.LogManager;
import com.yourorg.durablelogger.api.Logger;
import com.yourorg.durablelogger.core.DurableLogger;
import com.yourorg.durablelogger.storage.FileStorageAdapter;

import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        File wal = new File("data/wal.log");
        File checkpoint = new File("data/wal.check");
        File store = new File("data/store.log");

        var adapter = new FileStorageAdapter(store);
        var durable = new DurableLogger(adapter, wal, checkpoint, 10000, true, 500, 200);
        LogManager.init(adapter, durable);

        Logger logger = LogManager.get().getLogger(Main.class);

        logger.info("Application started");
        logger.log(com.yourorg.durablelogger.api.LogLevel.ERROR, "An error occurred");
        try {
            int x = 1/0;
        } catch (Exception ex) {
            logger.error("caught exception", ex);
        }

        // sleep to let flusher write
        Thread.sleep(1000);
        durable.close();
    }
}
