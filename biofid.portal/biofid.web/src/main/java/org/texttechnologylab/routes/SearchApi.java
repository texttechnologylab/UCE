package org.texttechnologylab.routes;

import freemarker.template.Configuration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.services.DatabaseService;
import org.texttechnologylab.services.UIMAService;
import spark.ModelAndView;
import spark.Route;
import freemarker.template.Configuration;
import spark.template.freemarker.FreeMarkerEngine;

import java.util.HashMap;
import java.util.List;

public class SearchApi {

    private UIMAService uimaService = null;
    private DatabaseService db = null;
    private Configuration freemakerConfig = Configuration.getDefaultConfiguration();

    public SearchApi(ApplicationContext serviceContext, Configuration freemakerConfig) {
        this.freemakerConfig = freemakerConfig;
        this.uimaService = serviceContext.getBean(UIMAService.class);
        this.db = serviceContext.getBean(DatabaseService.class);
    }

    public Route search = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var docs = this.db.searchForDocuments(15, 0);
        model.put("documents", docs);

        return new FreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "search/searchResult.ftl"));
    });
}
