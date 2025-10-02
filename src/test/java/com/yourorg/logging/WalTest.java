package com.yourorg.logging;

import com.yourorg.logging.api.LogEntry;
import com.yourorg.logging.api.LogLevel;
import com.yourorg.logging.core.WalReader;
import com.yourorg.logging.core.WalWriter;
import org.junit.jupiter.api.*;

import java.io.File;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WalTest {
    private File walFile;

    @BeforeEach
    void setup() {
        walFile = new File("target/test-wal.log");
        walFile.delete();
    }

    @Test
    void testWalWriteAndRead() throws Exception {
        try (WalWriter writer = new WalWriter(walFile, true)) {
            LogEntry entry = LogEntry.builder()
                    .service("test")
                    .level(LogLevel.INFO)
                    .message("hello wal")
                    .timestamp(Instant.now())
                    .build();
            writer.append(entry);
        }

        WalReader reader = new WalReader(walFile);
        List<LogEntry> entries = reader.readAll();

        assertEquals(1, entries.size());
        assertEquals("hello wal", entries.get(0).getMessage());
    }
}
