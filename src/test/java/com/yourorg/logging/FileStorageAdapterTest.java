package com.yourorg.logging;

import com.yourorg.logging.api.LogEntry;
import com.yourorg.logging.api.LogLevel;
import com.yourorg.logging.config.LoggerConfig;
import com.yourorg.logging.core.QueryRequest;
import com.yourorg.logging.core.QueryResult;
import com.yourorg.logging.storage.FileStorageAdapter;
import org.junit.jupiter.api.*;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class FileStorageAdapterTest {
    private File file;
    private FileStorageAdapter adapter;

    @BeforeEach
    void setup() throws Exception {
        file = new File("target/test-store.log");
        file.delete();
        LoggerConfig.Retention retention = new LoggerConfig.Retention();
        retention.rotateSizeMB = 10; // big enough not to rotate in test
        retention.maxDays = 7;
        adapter = new FileStorageAdapter(file, retention);
        adapter.start();
    }

    @AfterEach
    void cleanup() throws Exception {
        adapter.stop();
    }

    @Test
    void testAppendAndQuery() throws Exception {
        LogEntry e1 = LogEntry.builder()
                .service("test")
                .level(LogLevel.INFO)
                .message("Hello World")
                .timestamp(Instant.now())
                .build();
        adapter.append(List.of(e1));

        QueryRequest req = new QueryRequest(
                Instant.now().minusSeconds(10),
                Instant.now().plusSeconds(10),
                Optional.empty(),
                "Hello",
                10
        );
        QueryResult result = adapter.query(req);

        assertEquals(1, result.getEntries().size());
        assertEquals("Hello World", result.getEntries().get(0).getMessage());
    }
    @Test
    void testRotationHappens() throws Exception {
        LoggerConfig.Retention retention = new LoggerConfig.Retention();
        retention.rotateSizeMB = 0.001; // ~1 KB
        retention.maxDays = 7;

        File file = new File("target/test-rotate.log");
        file.delete();
        FileStorageAdapter adapter = new FileStorageAdapter(file, retention);
        adapter.start();

        // Write enough logs to trigger rotation
        for (int i = 0; i < 200; i++) {
            LogEntry e = LogEntry.builder()
                    .service("test")
                    .level(LogLevel.INFO)
                    .message("log " + i)
                    .timestamp(Instant.now())
                    .build();
            adapter.append(List.of(e));
        }

        adapter.stop();

        File dir = file.getParentFile();
        File[] rotated = dir.listFiles((d, name) -> name.startsWith("test-rotate.log."));
        assertTrue(rotated != null && rotated.length > 0, "Rotation should create extra file");
    }

}
