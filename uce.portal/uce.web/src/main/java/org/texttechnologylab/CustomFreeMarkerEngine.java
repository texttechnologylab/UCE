package org.texttechnologylab;

import org.texttechnologylab.freeMarker.RequestContextHolder;
import spark.ModelAndView;
import spark.Request;
import spark.TemplateEngine;

import java.util.HashMap;
import java.util.Map;

/**
 * A custom freemarker engine that allows us to inject additional logic - we use it e.g. to
 * inject the language resource object to every template.
 */
public class CustomFreeMarkerEngine extends TemplateEngine {

    private final freemarker.template.Configuration configuration;

    public CustomFreeMarkerEngine(freemarker.template.Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String render(ModelAndView modelAndView) {
        Map<String, Object> model = (Map)(modelAndView.getModel());

        if(model == null){
            model = new HashMap<>();
        }

        // Always inject the uceConfig
        model.put("uceConfig", RequestContextHolder.getUceConfig());

        // Add the LanguageResources object to the model if available in the request
        var languageResources = RequestContextHolder.getLanguageResources();
        if (languageResources != null) {
            model.put("languageResource", languageResources);
        }

        // Add the UceUser object to the model if available in the session
        var uceUser = RequestContextHolder.getAuthenticatedUceUser();
        if(uceUser != null){
            model.put("uceUser", uceUser);
        }

        try {
            return process(configuration, modelAndView.getViewName(), model);
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
}