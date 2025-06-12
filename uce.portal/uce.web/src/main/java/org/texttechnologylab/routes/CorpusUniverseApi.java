package org.texttechnologylab.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.*;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.ModelBase;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.rag.DocumentEmbedding;
import org.texttechnologylab.models.universe.CorpusUniverseNode;
import org.texttechnologylab.models.universe.UniverseLayer;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.services.RAGService;
import org.texttechnologylab.utils.ListUtils;
import spark.ModelAndView;
import spark.Route;

import java.util.*;

public class CorpusUniverseApi {
    private static final Logger logger = LogManager.getLogger(CorpusUniverseApi.class);
    private ApplicationContext context;
    private RAGService ragService;
    private PostgresqlDataInterface_Impl db;
    private Configuration freemarkerConfig;

    public CorpusUniverseApi(ApplicationContext serviceContext, Configuration freemarkerConfig) {
        this.context = serviceContext;
        this.ragService = serviceContext.getBean(RAGService.class);
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.freemarkerConfig = freemarkerConfig;
    }

    public Route getNodeInspectorContentView = ((request, response) -> {
        var model = new HashMap<String, Object>();
        //var corpusId = Long.parseLong(request.queryParams("corpusId"));
        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("documentId")),
                (ex) -> logger.error("Error: the url for the node inspector requires a 'documentId' query parameter. ", ex));
        if (documentId == null)
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));

        try {
            var document = db.getDocumentById(documentId);
            model.put("document", document);
        } catch (Exception ex) {
            logger.error("Error fetching the document for the node inspector with id: " + documentId, ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }

        return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "universe/nodeInspectorContent.ftl"));
    });

    public Route getCorpusUniverseView = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var corpusId = Long.parseLong(request.queryParams("corpusId"));
        var currentUniverseCenter = request.queryParams("currentCenter");

        model.put("corpusId", corpusId);
        model.put("currentCenter", currentUniverseCenter);
        try {
            // Later
        } catch (Exception ex) {
            logger.error("Error fetching a new corpus universe view");
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }

        return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "corpus/corpusUniverse.ftl"));
    });

    public Route fromCorpus = ((request, response) -> {
        var result = new HashMap<>();
        result.put("status", 200);
        var gson = new Gson();
        var requestBody = gson.fromJson(request.body(), Map.class);
        var corpusId = (long)Float.parseFloat(requestBody.get("corpusId").toString());

        var possibleCenter = requestBody.get("currentCenter").toString()
                .replace("[", "")
                .replace("]", "")
                .split(",");
        var currentCenter = ExceptionUtils.tryCatchLog(
                () -> ListUtils.convertStringArrayToFloatArray(possibleCenter),
                (ex) -> logger.error("Error parsing the current center of the universe. Center: " + Arrays.toString(possibleCenter), ex));
        var level = ExceptionUtils.tryCatchLog(
                () -> UniverseLayer.valueOf(requestBody.get("level").toString()),
                (ex) -> logger.error("Couldn't determine the desired level of the corpus universe.", ex));
        if (level == null || currentCenter == null) {
            result.replace("status", 500);
            return gson.toJson(result);
        }

        var nodes = new ArrayList<CorpusUniverseNode>();

        switch (level) {
            case DOCUMENTS:
                // First get the closest document embeddings to the current center
                var docEmbeddings = ExceptionUtils.tryCatchLog(
                        () -> ragService.getClosest3dDocumentEmbeddingsOfCorpus(currentCenter, 1000, corpusId),
                        (ex) -> logger.error("Error getting the closest 3d document embeddings.", ex));
                if (docEmbeddings == null) {
                    result.replace("status", 500);
                    break;
                }

                // Then, get the documents themselves for those embeddings
                var documents = ExceptionUtils.tryCatchLog(() -> db.getManyDocumentsByIds(
                                docEmbeddings.stream().map(de -> (int) de.getDocument_id()).toList()),
                        (ex) -> logger.error("Error fetching the documents belonging to embeddings with documentIds: "
                                + docEmbeddings.stream().map(DocumentEmbedding::getDocument_id), ex));
                if (documents == null){
                    result.replace("status", 500);
                    break;
                };

                // Foreach found embedding, we create a node
                for (var docEmbedding : docEmbeddings) {
                    var doc = documents.stream().filter(d -> d.getId() == docEmbedding.getDocument_id()).findFirst();
                    if (doc.isEmpty() || docEmbedding.getTsne3d() == null) continue;
                    var node = getCorpusUniverseNode(doc.get(), docEmbedding);
                    nodes.add(node);
                }
        }

        result.put("nodes", nodes);
        result.put("level", level);

        return gson.toJson(result);
    });

    public Route fromSearch = ((request, response) -> {
        var result = new HashMap<>();
        result.put("status", 200);
        var gson = new Gson();
        var languageResources = LanguageResources.fromRequest(request);

        var requestBody = gson.fromJson(request.body(), Map.class);
        var searchId = requestBody.get("searchId").toString();
        var level = ExceptionUtils.tryCatchLog(
                () -> UniverseLayer.valueOf(requestBody.get("level").toString()),
                (ex) -> logger.error("Couldn't determine the desired level of the corpus universe.", ex));
        if (!SessionManager.ActiveSearches.containsKey(searchId)) {
            logger.error("Error creating corpus universe from search.");
            result.replace("status", 500);
            return gson.toJson(result);
        }

        var activeSearchState = (SearchState) SessionManager.ActiveSearches.get(searchId);
        var search = new Search_DefaultImpl();
        search.fromSearchState(this.context, languageResources.getDefaultLanguage(), activeSearchState);
        var nodes = new ArrayList<CorpusUniverseNode>();

        switch (level) {
            case DOCUMENTS:
                var docEmbeddings = ExceptionUtils.tryCatchLog(
                        () -> ragService.getManyDocumentEmbeddingsOfDocuments(
                                search.getSearchState().getCurrentDocuments().stream().map(ModelBase::getId).toList()),
                        (ex) -> logger.error("Error fetching document embeddings of many documents.", ex));
                if (docEmbeddings == null) {
                    result.replace("status", 500);
                    break;
                }

                for (var doc : search.getSearchState().getCurrentDocuments()) {
                    var docEmbedding = docEmbeddings.stream().filter(e -> e.getDocument_id() == doc.getId()).findFirst();
                    if (docEmbedding.isEmpty() || docEmbedding.get().getTsne3d() == null) continue;
                    var node = getCorpusUniverseNode(doc, docEmbedding.get());
                    nodes.add(node);
                }
        }

        result.put("nodes", nodes);
        result.put("level", level);

        return gson.toJson(result);
    });

    private static CorpusUniverseNode getCorpusUniverseNode(Document doc, DocumentEmbedding docEmbedding) {
        var node = new CorpusUniverseNode();
        node.setDocumentId(doc.getId());
        if (doc.getDocumentKeywordDistribution() != null)
            node.setPrimaryTopic(doc.getDocumentKeywordDistribution().getYakeTopicOne());
        node.setTsne2d(docEmbedding.getTsne2d());
        node.setTsne3d(docEmbedding.getTsne3d());
        node.setTitle(doc.getDocumentTitle());
        node.setDocumentLength(doc.getFullText().length());
        return node;
    }

}
