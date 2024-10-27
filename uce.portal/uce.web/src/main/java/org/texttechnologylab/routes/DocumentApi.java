package org.texttechnologylab.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.CustomFreeMarkerEngine;
import org.texttechnologylab.SessionManager;
import org.texttechnologylab.config.CorpusConfig;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.search.SearchType;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.services.RAGService;
import org.texttechnologylab.services.UIMAService;
import spark.ModelAndView;
import spark.Route;

import java.util.HashMap;

public class DocumentApi {
    private UIMAService uimaService = null;
    private RAGService ragService = null;
    private PostgresqlDataInterface_Impl db = null;
    private static final Logger logger = LogManager.getLogger();
    private Configuration freemakerConfig = Configuration.getDefaultConfiguration();
    public DocumentApi(ApplicationContext serviceContext, Configuration freemakerConfig) {
        this.uimaService = serviceContext.getBean(UIMAService.class);
        this.ragService = serviceContext.getBean(RAGService.class);
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.freemakerConfig = freemakerConfig;
    }

    public Route getCorpusInspectorView = ((request, response) -> {
        var model = new HashMap<String, Object>();

        var corpusId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("id")),
                (ex) -> logger.error("Error: the url for the corpus inspector requires an 'id' query parameter that is the corpusId. ", ex));
        if(corpusId == null) return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(null, "defaultError.ftl"));

        try {
            var corpus = db.getCorpusById(corpusId);
            var corpusConfig = CorpusConfig.fromJson(corpus.getCorpusJsonConfig());
            var documentsCount = db.countDocumentsInCorpus(corpusId);

            model.put("corpus", corpus);
            model.put("corpusConfig", corpusConfig);
            model.put("documentsCount", documentsCount);

        } catch (Exception ex) {
            logger.error("Error getting the corpus inspector view.", ex);
            return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }

        return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "corpus/corpusInspector.ftl"));
    });

    public Route get3dGlobe = ((request, response) -> {
        var model = new HashMap<String, Object>();

        var id = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("id")),
                (ex) -> logger.error("Error: the url for the document 3d globe requires an 'id' query parameter that is the document id.", ex));
        if(id == null) return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(null, "defaultError.ftl"));

        try {
            // I've forgotten why I introduced this variable here?...
            //var type = request.queryParams("type");
            var document = db.getDocumentById(id);
            var data = db.getGlobeDataForDocument(id);
            var gson = new Gson();
            var dataJson = gson.toJson(data);

            model.put("document", document);
            model.put("data", data);
            model.put("jsonData", dataJson);
        } catch (Exception ex) {
            logger.error("Error getting the 3D globe of a document, returning default error view.", ex);
            return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }

        return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "corpus/globe.ftl"));
    });

    public Route getSingleDocumentReadView = ((request, response) -> {
        var model = new HashMap<String, Object>();

        var id = ExceptionUtils.tryCatchLog(() -> request.queryParams("id"),
                (ex) -> logger.error("Error: the url for the document reader requires an 'id' query parameter. " +
                        "Document reader can't be built.", ex));
        if(id == null) return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(null, "defaultError.ftl"));

        // Check if we have an searchId parameter. This is optional
        var searchId = ExceptionUtils.tryCatchLog(() -> request.queryParams("searchId"),
                (ex) -> logger.warn("Opening a document view but no searchId was provided. Currently, this shouldn't happen, but it didn't stop the procedure."));

        try {
            var doc = db.getCompleteDocumentById(Long.parseLong(id), 0, 10);
            logger.info("Loaded document from database with id " + id);
            model.put("document", doc);

            // If this document was opened from an active search, we can highlight the search tokens in the text
            // This is only optional and works fine even without the search tokens.
            if(searchId != null && SessionManager.ActiveSearches.containsKey(searchId)){
                var activeSearchState = SessionManager.ActiveSearches.get(searchId);
                // For SRL Search, there are no search tokens really. We will handle that exclusively later.
                if(activeSearchState.getSearchType() != SearchType.SEMANTICROLE)
                    model.put("searchTokens", String.join("[TOKEN]", activeSearchState.getSearchTokens()));
            }
        } catch (Exception ex) {
            logger.error("Error creating the document reader view for document with id: " + id, ex);
            return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }

        return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "reader/documentReaderView.ftl"));
    });

    public Route getPagesListView = ((request, response) -> {

        var model = new HashMap<String, Object>();

        var id = ExceptionUtils.tryCatchLog(() -> request.queryParams("id"),
                (ex) -> logger.error("Error: the url for the document pages list view requires an 'id' query parameter. ", ex));
        if(id == null) return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(null, "defaultError.ftl"));

        try{
            var skip = Integer.parseInt(request.queryParams("skip"));
            var doc = db.getCompleteDocumentById(Long.parseLong(id), skip, 10);
            var annotations = doc.getAllAnnotations(skip, 10);
            model.put("documentAnnotations", annotations);
            model.put("documentText", doc.getFullText());
            model.put("documentPages", doc.getPages(10, skip));
        } catch (Exception ex){
            logger.error("Error getting the pages list view - either the document couldn't be fetched (id=" + id + ") or its annotations.", ex);
            return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }

        return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "reader/components/pagesList.ftl"));
    });
}
