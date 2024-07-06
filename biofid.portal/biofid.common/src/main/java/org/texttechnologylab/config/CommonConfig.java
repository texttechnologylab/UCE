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
    public String getUniversityCollectionBaseUrl(){
        return getProperty("university.collection.base.url");
    }
    public String getPostgresqlProperty(String prop) {return getProperty("postgresql." + prop);}
    public String getGbifOccurrencesSearchUrl(){
        return getProperty("gbif.occurrences.search.url");
    }
    public String getRAGWebserverBaseUrl(){
        return getProperty("rag.webserver.base.url");
    }
    public String getRAGModel(){
        return getProperty("rag.model");
    }
    public String getTemplatesLocation(){
        return getProperty("templates.location");
    }
    public String getPublicLocation(){
        return getProperty("external.public.location");
    }
}
