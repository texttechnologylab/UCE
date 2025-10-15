package org.texttechnologylab.uce.web.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.models.ModelBase;
import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.common.models.rag.DocumentEmbedding;
import org.texttechnologylab.uce.common.models.universe.CorpusUniverseNode;
import org.texttechnologylab.uce.common.models.universe.UniverseLayer;
import org.texttechnologylab.uce.common.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.uce.common.services.RAGService;
import org.texttechnologylab.uce.common.utils.ListUtils;
import org.texttechnologylab.uce.search.SearchState;
import org.texttechnologylab.uce.search.Search_DefaultImpl;
import org.texttechnologylab.uce.web.LanguageResources;
import org.texttechnologylab.uce.web.SessionManager;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CorpusUniverseApi implements UceApi {
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

    public void getNodeInspectorContentView(Context ctx) {
        var model = new HashMap<String, Object>();
        //var corpusId = Long.parseLong(request.queryParams("corpusId"));
        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(ctx.queryParam("documentId")),
                (ex) -> logger.error("Error: the url for the node inspector requires a 'documentId' query parameter. ", ex));
        if (documentId == null) {
            ctx.render("defaultError.ftl");
            return;
        }

        try {
            var document = db.getDocumentById(documentId);
            model.put("document", document);
        } catch (Exception ex) {
            logger.error("Error fetching the document for the node inspector with id: " + documentId, ex);
            ctx.render("defaultError.ftl");
            return;
        }

        ctx.render("universe/nodeInspectorContent.ftl", model);
    }

    public void getCorpusUniverseView(Context ctx) {
        var model = new HashMap<String, Object>();
        var corpusId = Long.parseLong(ctx.queryParam("corpusId"));
        var currentUniverseCenter = ctx.queryParam("currentCenter");

        model.put("corpusId", corpusId);
        model.put("currentCenter", currentUniverseCenter);
        try {
            // Later
        } catch (Exception ex) {
            logger.error("Error fetching a new corpus universe view");
            ctx.render("defaultError.ftl");
            return;
        }

        ctx.render("corpus/corpusUniverse.ftl", model);
    }

    public void fromCorpus(Context ctx) {
        var result = new HashMap<>();
        result.put("status", 200);
        var gson = new Gson();
        var requestBody = gson.fromJson(ctx.body(), Map.class);
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
            ctx.json(result);
            return;
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

        ctx.json(result);
    }

    public void fromSearch(Context ctx) throws IOException, URISyntaxException {
        var result = new HashMap<>();
        result.put("status", 200);
        var gson = new Gson();
        var languageResources = LanguageResources.fromRequest(ctx);

        var requestBody = gson.fromJson(ctx.body(), Map.class);
        var searchId = requestBody.get("searchId").toString();
        var level = ExceptionUtils.tryCatchLog(
                () -> UniverseLayer.valueOf(requestBody.get("level").toString()),
                (ex) -> logger.error("Couldn't determine the desired level of the corpus universe.", ex));
        if (!SessionManager.ActiveSearches.containsKey(searchId)) {
            logger.error("Error creating corpus universe from search.");
            result.replace("status", 500);
            ctx.json(result);
            return;
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

        ctx.json(result);
    }

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
