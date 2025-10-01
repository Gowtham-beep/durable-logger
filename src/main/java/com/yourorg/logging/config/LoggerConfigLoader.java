package com.yourorg.logging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.InputStream;

public class LoggerConfigLoader {
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    // Load from external file path
    public static LoggerConfig load(String path) throws Exception {
        return mapper.readValue(new File(path), LoggerConfig.class);
    }

    // Load from resources (classpath)
    public static LoggerConfig loadFromClasspath(String resourceName) throws Exception {
        try (InputStream in = LoggerConfigLoader.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IllegalStateException("Config file " + resourceName + " not found in resources");
            }
            return mapper.readValue(in, LoggerConfig.class);
        }
    }
}
