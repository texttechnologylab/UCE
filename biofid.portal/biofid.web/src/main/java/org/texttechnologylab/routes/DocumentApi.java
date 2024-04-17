package org.texttechnologylab.routes;

import com.google.gson.Gson;
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
            var type = request.queryParams("type");
            var id = Long.parseLong(request.queryParams("id"));

            var document = db.getDocumentById(id);
            var data = db.getGlobeDataForDocument(id);
            var gson = new Gson();
            var dataJson = gson.toJson(data);

            model.put("document", document);
            model.put("data", data);
            model.put("jsonData", dataJson);
        } catch (Exception ex) {
            // TODO: Logging
            model.put("data", "");
        }

        return new FreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "corpus/globe.ftl"));
    });

    public Route getSingleDocumentReadView = ((request, response) -> {

        var id = request.queryParams("id");

        var doc = db.getCompleteDocumentById(Long.parseLong(id), 0, 10);
        System.out.println("Loaded document from database with id " + id);
        var model = new HashMap<String, Object>();
        model.put("document", doc);

        return new FreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "reader/documentReaderView.ftl"));
    });

    public Route getPagesListView = ((request, response) -> {

        var id = request.queryParams("id");
        var skip = Integer.parseInt(request.queryParams("skip"));

        var doc = db.getCompleteDocumentById(Long.parseLong(id), skip, 10);

        var model = new HashMap<String, Object>();
        var annotations = doc.getAllAnnotations(skip, 10);
        model.put("documentAnnotations", annotations);
        model.put("documentText", doc.getFullText());
        model.put("documentPages", doc.getPages(10, skip));

        return new FreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "reader/components/pagesList.ftl"));
    });
}
