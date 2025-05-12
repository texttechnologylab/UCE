package org.texttechnologylab.config;

import org.texttechnologylab.models.geonames.FeatureCode;
import org.texttechnologylab.utils.SystemStatus;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CommonConfig {

    private final Properties properties;
    private final List<FeatureCode> geoNamesFeatureCodes;

    public CommonConfig() {
        properties = new Properties();

        // Load in the common conf in the java properties() style
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

        // Now load in the GeoNames feature Codes.
        try {
            // Load the .conf file from the resources directory
            this.geoNamesFeatureCodes = new ArrayList<>();
            var inputStream = getClass().getClassLoader().getResourceAsStream("geonames_featurecodes_en.txt");
            if (inputStream != null) {
                var reader = new BufferedReader(new InputStreamReader(inputStream));
                while (reader.ready()) {
                    var code = FeatureCode.fromFileLine(reader.readLine());
                    if (code != null) this.geoNamesFeatureCodes.add(code);
                }

            } else {
                throw new RuntimeException("geonames_featurecodes_en.txt not found not found in the classpath");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading geonames_featurecodes_en.txt: " + e.getMessage());
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getUniversityBotanikBaseUrl() {
        return getProperty("university.botanik.base.url");
    }

    public String getUniversityCollectionBaseUrl() {
        return getProperty("university.collection.base.url");
    }

    public String getPostgresqlProperty(String prop) {
        return getProperty("postgresql." + prop);
    }

    public String getGbifOccurrencesSearchUrl() {
        return getProperty("gbif.occurrences.search.url");
    }

    public String getSparqlHost() {
        return getProperty("sparql.host");
    }

    public String getSparqlEndpoint() {
        return getProperty("sparql.endpoint");
    }

    public long getSessionJobInterval() {
        return Long.parseLong(getProperty("session.job.interval"));
    }

    public long getSystemJobInterval() {
        return Long.parseLong(getProperty("system.job.interval"));
    }

    public String getRAGWebserverBaseUrl() {
        return getProperty("rag.webserver.base.url");
    }

    public String getRAGModel() {
        return SystemStatus.UceConfig.getSettings().getRag().getModel();
    }

    public String getRagOpenAIApiKey() {
        return SystemStatus.UceConfig.getSettings().getRag().getApiKey();
    }

    public boolean getLogToDb() {
        return Boolean.parseBoolean(getProperty("log.db"));
    }

    public String getTemplatesLocation() {
        return getProperty("templates.location");
    }

    public String getPublicLocation() {
        return getProperty("external.public.location");
    }

    public String getDatabaseScriptsLocation() {
        return getProperty("database.scripts.location");
    }

    public String getUceVersion() {
        return getProperty("uce.version");
    }

    public List<FeatureCode> getGeoNamesFeatureCodesList() {
        return this.geoNamesFeatureCodes;
    }
}
