package com.yourorg.logging.core;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

public class Checkpoint{
    private final File file;
    public Checkpoint(File file){
        this.file=file;
    }
    public synchronized void write(String lastUuid)throws Exception{
        file.getParentFile().mkdirs();
        Files.writeString(file.toPath(),lastUuid,StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    public synchronized String read() throws Exception{
        if(!file.exists()) return null;
        return Files.readString(file.toPath(),StandardCharsets.UTF_8).trim();
    }
}