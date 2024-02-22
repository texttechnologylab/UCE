package org.texttechnologylab.routes;

import freemarker.template.Configuration;
import org.hibernate.dialect.Database;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.services.DatabaseService;
import org.texttechnologylab.services.UIMAService;
import spark.ModelAndView;
import spark.Route;
import spark.template.freemarker.FreeMarkerEngine;

import javax.xml.crypto.Data;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class DocumentApi {

    private UIMAService uimaService = null;
    private DatabaseService db = null;

    private Configuration freemakerConfig = Configuration.getDefaultConfiguration();

    public DocumentApi(ApplicationContext serviceContext, Configuration freemakerConfig) {
        this.uimaService = serviceContext.getBean(UIMAService.class);
        this.db = serviceContext.getBean(DatabaseService.class);
        this.freemakerConfig = freemakerConfig;
    }

    public Route getSingleDocumentReadView = ((request, response) -> {

        var id = request.queryParams("id");

        var doc = db.getCompleteDocumentById(Long.parseLong(id));

        var model = new HashMap<String, Object>();
        model.put("document", doc);

        return new FreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "reader/documentReaderView.ftl"));
    });
}
