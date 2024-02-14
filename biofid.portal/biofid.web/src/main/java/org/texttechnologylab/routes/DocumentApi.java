package org.texttechnologylab.routes;

import freemarker.template.Configuration;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.services.UIMAService;
import spark.ModelAndView;
import spark.Route;
import spark.template.freemarker.FreeMarkerEngine;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class DocumentApi {

    private UIMAService uimaService = null;

    private Configuration freemakerConfig = Configuration.getDefaultConfiguration();

    public DocumentApi(ApplicationContext serviceContext, Configuration freemakerConfig) {
        this.uimaService = serviceContext.getBean(UIMAService.class);
        this.freemakerConfig = freemakerConfig;
    }

    public Route getSingleDocumentReadView = ((request, response) -> {

        var modelId = request.queryParams("modelId");

        // TODO: Fetch the document correctly here with the database service
        var random = new Random();
        var doc = uimaService.TestDocument.get(random.nextInt(uimaService.TestDocument.size()));

        var model = new HashMap<String, Object>();
        model.put("document", doc);

        return new FreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "reader/documentReaderView.ftl"));
    });
}
