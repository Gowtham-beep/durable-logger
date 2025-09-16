package com.yourorg.logging.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.logging.api.LogEntry;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.ArrayList;
import java.util.zip.CRC32;

public class WalReader {
    private final FileChannel ch;
    private final ObjectMapper om = new ObjectMapper();
    public WalReader(java.io.File file) throws Exception{
        this.ch = new FileInputStream(file).getChannel();
    }
    public List<LogEntry> readAll() throws Exception{
        List<LogEntry> out = new ArrayList<>();
        long final ObjectMapper om = new ObjectMapper();
        ByteBuffer headerBuf = ByteBuffer.allocate()
    }
}