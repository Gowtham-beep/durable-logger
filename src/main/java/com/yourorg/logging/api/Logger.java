package com.yourorg.logging.api;

import java.util.Map;

public interface Logger {
    void log(LogLevel level,String message);
    void log(LogLevel level,String message,Map<String,Object> fields);
    void error(String message,Throwable t);
    void info(String message);
    void warn(String message);
}