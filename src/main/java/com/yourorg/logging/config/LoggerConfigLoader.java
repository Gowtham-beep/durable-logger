package com.yourorg.logging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;

public class LoggerConfigLoader {
    public static LoggerConfig load(String path) throws Exception {
        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        return om.readValue(new File(path), LoggerConfig.class);
    }
}
