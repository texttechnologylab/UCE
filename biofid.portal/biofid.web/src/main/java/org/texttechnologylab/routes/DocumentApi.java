package org.texttechnologylab.routes;

import org.springframework.context.ApplicationContext;
import org.texttechnologylab.services.UIMAService;
import spark.Route;

public class DocumentApi {

    private final UIMAService uimaService;

    public DocumentApi(ApplicationContext serviceContext){
        this.uimaService = serviceContext.getBean(UIMAService.class);
    }

    public Route getSingleDocument = ((request, response) -> {
        return "Get a single document";
    });
}
