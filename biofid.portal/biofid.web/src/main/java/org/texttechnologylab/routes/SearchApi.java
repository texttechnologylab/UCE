package org.texttechnologylab.routes;

import freemarker.template.Configuration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.services.UIMAService;
import spark.ModelAndView;
import spark.Route;
import freemarker.template.Configuration;
import spark.template.freemarker.FreeMarkerEngine;

import java.util.HashMap;
import java.util.List;

public class SearchApi {

    private final UIMAService uimaService;
    private Configuration freemakerConfig = Configuration.getDefaultConfiguration();
    ;
    private List<Document> testDocs;

    public SearchApi(ApplicationContext serviceContext, Configuration freemakerConfig) {
        this.freemakerConfig = freemakerConfig;
        this.uimaService = serviceContext.getBean(UIMAService.class);
        // TODO: This is only testing for now.
        this.testDocs = uimaService.XMIFolderToDocuments("C:\\kevin\\projects\\biofid\\test_data\\2020_02_10");
    }

    public Route search = ((request, response) -> {
        var model = new HashMap<String, Object>();
        model.put("documents", this.testDocs);

        return new FreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "search/searchResult.ftl"));
    });
}
