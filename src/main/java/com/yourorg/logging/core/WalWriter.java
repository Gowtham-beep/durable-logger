package com.yourorg.logging.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.logging.api.LogEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;

public class WalWriter implements AutoCloseable{
    private static final int MAGIC = 0X4C4F4701;
    private final FileChannel ch;
    private final ObjectMapper om = new ObjectMapper();
    private final boolean fsyncOnAppend;

    public WalWriter(File file,boolean fsyncOnAppend) throws Exception{
        file.getParentFile().mkdirs();
        this.ch = new FileOutputStream(file,true).getChannel();
        this.fsyncOnAppend = fsyncOnAppend;
    }
    public synchronized void append(LogEntry e) throws Exception{
        byte[] payload = om.writeValueAsBytes(e);
        CRC32 crc = new CRC32(); crc.update(payload);
        int recordLen = payload.length;
        ByteBuffer buf  = ByteBuffer.allocate(4+8+recordLen+4);
        buf.putInt(MAGIC);
        buf.putLong(recordLen);
        buf.put(payload);
        buf.putInt((int)crc.getValue());
        buf.flip();
        while (buf.hasRemaining()) ch.write(buf);
        if(fsyncOnAppend) ch.force(false);
    }
    @Override
    public void close() throws Exception{
        ch.close();
    }
}