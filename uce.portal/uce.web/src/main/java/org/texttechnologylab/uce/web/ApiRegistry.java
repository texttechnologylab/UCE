package org.texttechnologylab.uce.web;

import freemarker.template.Configuration;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.uce.web.routes.*;

import java.util.Map;

public class ApiRegistry {
    private final Map<Class<? extends UceApi>, UceApi> apis;

    public ApiRegistry(ApplicationContext context, Configuration configuration, int DUUIInputCounter) {
        this.apis = Map.of(
                SearchApi.class, new SearchApi(context, configuration),
                DocumentApi.class, new DocumentApi(context, configuration),
                RAGApi.class, new RAGApi(context, configuration),
                CorpusUniverseApi.class, new CorpusUniverseApi(context, configuration),
                WikiApi.class, new WikiApi(context, configuration),
                ImportExportApi.class, new ImportExportApi(context),
                AnalysisApi.class, new AnalysisApi(context, configuration, DUUIInputCounter),
                MapApi.class, new MapApi(context, configuration),
                AuthenticationApi.class, new AuthenticationApi(context, configuration),
                McpApi.class, new McpApi(context, configuration)
        );
    }

    @SuppressWarnings("unchecked")
    public <T extends UceApi> T get(Class<T> clazz) {
        return (T) apis.get(clazz);
    }

    public Map<Class<? extends UceApi>, UceApi> getAll() {
        return apis;
    }
}
