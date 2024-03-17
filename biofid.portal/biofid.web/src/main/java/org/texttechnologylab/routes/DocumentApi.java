package org.texttechnologylab.routes;

import freemarker.template.Configuration;
import org.bson.Document;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.services.DatabaseService;
import org.texttechnologylab.services.UIMAService;
import spark.ModelAndView;
import spark.Route;
import spark.template.freemarker.FreeMarkerEngine;

import java.util.HashMap;

public class DocumentApi {

    private UIMAService uimaService = null;
    private DatabaseService db = null;

    private Configuration freemakerConfig = Configuration.getDefaultConfiguration();

    public DocumentApi(ApplicationContext serviceContext, Configuration freemakerConfig) {
        this.uimaService = serviceContext.getBean(UIMAService.class);
        this.db = serviceContext.getBean(DatabaseService.class);
        this.freemakerConfig = freemakerConfig;
    }

    public Route getCorpusWorldView = ((request, response) -> {
        var model = new HashMap<String, Object>();
        try {
            var document = db.getDocumentById(1);
            var data = db.getGlobeDataForDocument(1);
            var dataDoc = new Document();
            dataDoc.append("occurrences", data);

            model.put("document", document);
            model.put("data", dataDoc.toJson());
        } catch (Exception ex) {
            // TODO: Logging
            model.put("data", "");
        }
        return new FreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "corpus/globe.ftl"));
    });

    public Route getSingleDocumentReadView = ((request, response) -> {

        var id = request.queryParams("id");

        var doc = db.getCompleteDocumentById(Long.parseLong(id));

        var model = new HashMap<String, Object>();
        model.put("document", doc);

        return new FreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "reader/documentReaderView.ftl"));
    });
}
