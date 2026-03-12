package org.texttechnologylab.uce.common.config;

import com.google.gson.Gson;
import org.keycloak.authorization.client.Configuration;
import org.texttechnologylab.uce.common.models.geonames.FeatureCode;
import org.texttechnologylab.uce.common.utils.SystemStatus;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class CommonConfig {
    private static final Path EXTERNAL_COMMON_CONFIG_PATH = Path.of("/app/config/commonEmpty.conf");
    private static final Path LEGACY_COMMON_CONFIG_PATH = Path.of("uce.common/src/main/resources/commonEmpty.conf");

    private final Properties properties;
    private final List<FeatureCode> geoNamesFeatureCodes;

    public CommonConfig() {
        properties = new Properties();

//        // Load in the common conf in the java properties() style
//        try {
//            // Load the .conf file from the resources directory
//            // commonEmpty.conf is for loading with DUUI cas importer
//            Path external = Path.of("/app/config/commonEmpty.conf");
//            var inputStream = getClass().getClassLoader().getResourceAsStream("commonEmpty.conf");
//            if (inputStream != null) {
//                // Check inputStream is empty, if so load the default common.conf from the classpath
//                if (inputStream.available() == 0) {
//                    inputStream = getClass().getClassLoader().getResourceAsStream("common.conf");
//                    if (inputStream == null) {
//                        throw new RuntimeException("common.conf not found not found in the classpath");
//                    }
//                }
//                properties.load(inputStream);
//            } else {
//                throw new RuntimeException("common.conf not found not found in the classpath");
//            }
//        } catch (Exception e) {
//            throw new RuntimeException("Error loading config.conf: " + e.getMessage());
//        }

        try {
            InputStream inputStream = resolveCommonConfigInputStream();
            try (InputStream is = inputStream) {
                properties.load(is);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading common config: " + e.getMessage(), e);
        }

        applyEnvironmentOverrides();

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

    private void applyEnvironmentOverrides() {
        // Convention: env var names mirror property keys:
        // - dots become underscores, all uppercase
        // Example: keycloak.auth_server_url -> KEYCLOAK_AUTH_SERVER_URL
        //
        // Only overrides existing keys to avoid silently accepting typos.
        var env = System.getenv();
        for (var key : properties.stringPropertyNames()) {
            var envKey = toEnvVarName(key);
            var value = env.get(envKey);
            if (value == null || value.isBlank()) {
                value = getEnvAliasValue(key, env);
            }
            if (value != null && !value.isBlank()) {
                properties.setProperty(key, value);
            }
        }
    }

    private static String getEnvAliasValue(String propertyKey, Map<String, String> env) {
        // Backward-/cross-compat aliases so users don't need to define the same concept multiple times.
        //
        // Keycloak:
        // - keycloak.realm           <- KC_REALM
        // - keycloak.client          <- KC_CLIENT_ID
        // - keycloak.auth_server_url <- UCE_AUTH_PUBLIC_URL, then KC_BASE_URL
        //
        // PostgreSQL (app runtime):
        // - postgresql.hibernate.connection.username <- POSTGRES_USER
        // - postgresql.hibernate.connection.password <- POSTGRES_PASSWORD
        return switch (propertyKey) {
            case "keycloak.realm" -> env.get("KC_REALM");
            case "keycloak.client" -> env.get("KC_CLIENT_ID");
            case "keycloak.auth_server_url" -> {
                var v = env.get("UCE_AUTH_PUBLIC_URL");
                if (v == null || v.isBlank()) {
                    v = env.get("KC_BASE_URL");
                }
                yield v;
            }
            case "postgresql.hibernate.connection.username" -> env.get("POSTGRES_USER");
            case "postgresql.hibernate.connection.password" -> env.get("POSTGRES_PASSWORD");
            default -> null;
        };
    }

    private static String toEnvVarName(String propertyKey) {
        return propertyKey
                .replace('.', '_')
                .replace('-', '_')
                .toUpperCase();
    }

    private InputStream resolveCommonConfigInputStream() throws Exception {
        for (var path : List.of(EXTERNAL_COMMON_CONFIG_PATH, LEGACY_COMMON_CONFIG_PATH)) {
            if (isNonEmptyFile(path)) {
                return Files.newInputStream(path);
            }
        }

        var classpathOverride = getClass().getClassLoader().getResourceAsStream("commonEmpty.conf");
        if (classpathOverride != null) {
            if (classpathOverride.available() > 0) {
                return classpathOverride;
            }
            classpathOverride.close();
        }

        var defaultConfig = getClass().getClassLoader().getResourceAsStream("common.conf");
        if (defaultConfig == null) {
            throw new RuntimeException("common.conf not found in the classpath");
        }
        return defaultConfig;
    }

    private static boolean isNonEmptyFile(Path path) throws Exception {
        return Files.exists(path) && Files.isRegularFile(path) && Files.size(path) > 0;
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

    public String getEmbeddingWebserverBaseUrl() {
        var embeddingUrl = getProperty("embedding.webserver.base.url");
        if (embeddingUrl != null && !embeddingUrl.isBlank()) {
            return embeddingUrl;
        }
        return getRAGWebserverBaseUrl();
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
