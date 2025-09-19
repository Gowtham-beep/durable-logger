package com.yourorg.logging.api;

import com.yourorg.logging.storage.StorageAdapter;

public final class LogManager{
    private static volatile LogManager INSTANCE;
    private final StorageAdapter adapter;
    private final com.yourorg.logging.core.DurableLogger logger;

    private LogManager(StorageAdapter adapter,com.yourorg.logging.core.DurableLogger logger){
        this.adapter=adapter; this.logger=logger;
    }
    public static synchronized void init(StorageAdapter adapter,com.yourorg.logging.core.DurableLogger logger){
        INSTANCE = new LogManager(adapter,logger);
    }
    public static LogManager get(){return  INSTANCE;}

    public Logger getLogger(Class<?> cls){
        return logger.forClass(cls);
    }
    public com.yourorg.logging.core.DurableLogger getCorelogger(){return logger;}

}