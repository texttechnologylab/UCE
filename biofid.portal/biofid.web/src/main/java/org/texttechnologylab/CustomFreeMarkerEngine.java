package org.texttechnologylab;

import org.texttechnologylab.freeMarker.RequestContextHolder;
import spark.ModelAndView;
import spark.Request;
import spark.TemplateEngine;

import java.util.HashMap;
import java.util.Map;

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

        // Add the LanguageResources object to the model if available in the request
        var languageResources = RequestContextHolder.getLanguageResources();
        if (languageResources != null) {
            model.put("languageResource", languageResources);
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