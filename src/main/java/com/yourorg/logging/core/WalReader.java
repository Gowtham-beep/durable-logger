package com.yourorg.logging.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.logging.api.LogEntry;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.ArrayList;
import java.util.zip.CRC32;

public class WalReader implements AutoCloseable {
    private final FileChannel ch;
    private final ObjectMapper om = new ObjectMapper();

    public WalReader(File file) throws Exception {
        this.ch = new FileInputStream(file).getChannel();
    }

    public List<LogEntry> readAll() throws Exception {
        List<LogEntry> out = new ArrayList<>();
        long pos = 0;
        ByteBuffer headerBuf = ByteBuffer.allocate(4 + 8); // MAGIC + LEN

        while (true) {
            headerBuf.clear();
            int n = ch.read(headerBuf, pos);
            if (n <= 0) break;
            if (n < headerBuf.capacity()) break; // truncated header

            headerBuf.flip();
            int magic = headerBuf.getInt();
            if (magic != 0x4C4F4701) break; // invalid magic
            long len = headerBuf.getLong();
            if (len <= 0 || len > (1 << 28)) break; // sanity (max ~256MB)

            // read payload
            ByteBuffer payload = ByteBuffer.allocate((int) len);
            int read = ch.read(payload, pos + headerBuf.capacity());
            if (read < len) break; // truncated payload
            payload.flip();
            byte[] payloadBytes = new byte[(int) len];
            payload.get(payloadBytes);

            // read crc
            ByteBuffer crcBuf = ByteBuffer.allocate(4);
            int r2 = ch.read(crcBuf, pos + headerBuf.capacity() + len);
            if (r2 < 4) break; // truncated crc
            crcBuf.flip();
            int crcVal = crcBuf.getInt();

            // verify crc
            CRC32 crc = new CRC32();
            crc.update(payloadBytes);
            if ((int) crc.getValue() != crcVal) break; // corruption

            // deserialize
            LogEntry e = om.readValue(payloadBytes, LogEntry.class);
            out.add(e);

            // advance pointer
            pos += headerBuf.capacity() + len + 4;
        }
        return out;
    }

    @Override
    public void close() throws Exception {
        ch.close();
    }
}
