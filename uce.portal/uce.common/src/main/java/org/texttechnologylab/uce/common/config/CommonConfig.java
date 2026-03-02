package org.texttechnologylab.uce.common.config;

import com.google.gson.Gson;
import org.keycloak.authorization.client.Configuration;
import org.texttechnologylab.uce.common.models.geonames.FeatureCode;
import org.texttechnologylab.uce.common.utils.SystemStatus;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    public int getLocationEnrichmentLimit() {return Integer.parseInt(getPostgresqlProperty("enrichment.location.max"));}

    public String getGbifOccurrencesSearchUrl() {
        return getProperty("gbif.occurrences.search.url");
    }

    public String getSparqlHost() {
        return getProperty("sparql.host");
    }

    public String getSparqlEndpoint() {
        return getProperty("sparql.endpoint");
    }

    public int getSparqlMaxEnrichment() { return Integer.parseInt(getProperty("sparql.max.enrichment")); }

    public long getSessionJobInterval() {
        return Long.parseLong(getProperty("session.job.interval"));
    }

    public long getSystemJobInterval() {
        return Long.parseLong(getProperty("system.job.interval"));
    }

    public String getRAGWebserverBaseUrl() {
        return getProperty("rag.webserver.base.url");
    }

    public Configuration getKeyCloakConfiguration() {
        return new Configuration(
                getProperty("keycloak.auth_server_url"),
                getProperty("keycloak.realm"),
                getProperty("keycloak.client"),
                Map.of("secret", getProperty("keycloak.credentials.secret")),
                null
        );
    }

    public String getEmbeddingBackend() {
        if (SystemStatus.UceConfig == null) {
            return null;
        }
        return SystemStatus.UceConfig.getSettings().getEmbeddings().getBackend();
    }

    public Map<String, Object> getEmbeddingParameters() {
        if (SystemStatus.UceConfig == null) {
            return null;
        }
        return SystemStatus.UceConfig.getSettings().getEmbeddings().getParameters();
    }

    public String getEmbeddingParametersString() {
        if (SystemStatus.UceConfig == null) {
            return null;
        }
        return new Gson().toJson(getEmbeddingParameters());
    }

    public long getEmbeddingTimeout() {
        if (SystemStatus.UceConfig == null) {
            // Default was 2 seconds before the config was introduced
            return 200;
        }
        return SystemStatus.UceConfig.getSettings().getEmbeddings().getTimeout();
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

    public boolean useExternalPublicLocation() {
        return Boolean.parseBoolean(getProperty("external.public.use"));
    }

    public String getDatabaseScriptsLocation() {
        return getProperty("database.scripts.location");
    }

    public String getUceVersion() {
        return getProperty("uce.version");
    }

    public String getMinioEndpoint() {
        return getProperty("minio.endpoint");
    }

    public String getMinioBucket() {
        return getProperty("minio.bucket");
    }

    public String getMinioKey() {
        return getProperty("minio.username");
    }

    public String getMinioSecret() {
        return getProperty("minio.pwd");
    }

    public List<FeatureCode> getGeoNamesFeatureCodesList() {
        return this.geoNamesFeatureCodes;
    }
}
