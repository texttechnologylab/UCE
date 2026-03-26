package org.texttechnologylab.uce.web;

import freemarker.template.Configuration;
import io.javalin.http.Context;
import io.javalin.rendering.FileRenderer;
import org.jetbrains.annotations.NotNull;
import org.texttechnologylab.uce.web.freeMarker.RequestContextHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * A custom freemarker engine that allows us to inject additional logic - we use it e.g. to
 * inject the language resource object to every template.
 */
public class CustomFreeMarkerEngine implements FileRenderer {

    private final Configuration configuration;

    public CustomFreeMarkerEngine(freemarker.template.Configuration configuration) {
        this.configuration = configuration;
    }

    public String render(String templatePath, Map<String, Object> model) {
        Map<String, Object> mutableModel = model == null ? new HashMap<>() : new HashMap<>(model);

        // Always inject the uceConfig
        mutableModel.put("uceConfig", RequestContextHolder.getUceConfig());

        // Add the LanguageResources object to the model if available in the request
        var languageResources = RequestContextHolder.getLanguageResources();
        if (languageResources != null) {
            mutableModel.put("languageResource", languageResources);
        }

        // Add the UceUser object to the model if available in the session
        var uceUser = RequestContextHolder.getAuthenticatedUceUser();
        if(uceUser != null){
            mutableModel.put("uceUser", uceUser);
        }

        try {
            return process(configuration, templatePath, mutableModel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String process(freemarker.template.Configuration configuration, String templateName, Object model) {
        try {
            freemarker.template.Template template = configuration.getTemplate(templateName);
            return renderTemplate(template, model);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String renderTemplate(freemarker.template.Template template, Object model) {
        try {
            java.io.StringWriter writer = new java.io.StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public String render(@NotNull String s, @NotNull Map<String, ?> map, @NotNull Context context) {
        return render(s, (Map<String, Object>) map) ;
    }
}