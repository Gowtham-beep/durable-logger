package com.yourorg.logging.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.logging.api.LogEntry;
import com.yourorg.logging.core.QueryRequest;
import com.yourorg.logging.core.QueryResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FileStorageAdapter implements StorageAdapter {
    private final File file;
    private final ObjectMapper om = new ObjectMapper();
    private BufferedWriter writer;

    public FileStorageAdapter(File file){
        this.file=file;
    }
    @Override
    public void start() throws Exception{
        file.getParentFile().mkdirs();
        writer = new BufferedWriter(new FileWriter(file,true));
    }
    @Override
    public void stop() throws Exception{
        if(writer!=null) writer.close();
    }
    @Override
    public synchronized void append(List<LogEntry> entries) throws Exception{
        for(LogEntry e: entries){
            String json = om.writeValueAsString(e);
            writer.write(json);
            writer.newLine();
        }
        writer.flush();
    }
    @Override
    public QueryResult query(QueryRequest request){
        List<LogEntry> out = new ArrayList<>();
        try(Stream<String> lines = Files.lines(file.toPath())){
            lines.forEach(line->{
                try{
                    LogEntry e = om.readValue(line,LogEntry.class);
                    Instant ts = e.getTimestamp();
                    if(!ts.isBefore(request.getFrom()) && !ts.isAfter(request.getTo())){
                        if(request.getText() == null || e.getMessage().contains(request.getText())){
                            out.add(e);
                        }
                    }
                }catch (Exception ex){}
            });
        }catch(Exception ex){throw  new RuntimeException(ex);}
        return new QueryResult(out,out.size());
    }
}