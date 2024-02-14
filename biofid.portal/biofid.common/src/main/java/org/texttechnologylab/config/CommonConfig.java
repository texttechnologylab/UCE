package org.texttechnologylab.config;

import java.util.Properties;

public class CommonConfig {

    private final Properties properties;

    public CommonConfig(){
        properties = new Properties();
        try {
            // Load the .conf file from the resources directory
            var inputStream = getClass().getClassLoader().getResourceAsStream("common.conf");
            if (inputStream != null) {
                properties.load(inputStream);
            } else {
                throw new RuntimeException("common.conf not found not found in the classpath");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading config.conf: " + e.getMessage());
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getUniversityBotanikBaseUrl(){
        return getProperty("university.botanik.base.url");
    }

}
