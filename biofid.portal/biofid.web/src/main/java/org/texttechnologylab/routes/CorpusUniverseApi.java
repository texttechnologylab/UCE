package org.texttechnologylab.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.CustomFreeMarkerEngine;
import org.texttechnologylab.Search_DefaultImpl;
import org.texttechnologylab.models.universe.CorpusUniverseNode;
import org.texttechnologylab.models.universe.UniverseLayer;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.services.RAGService;
import org.texttechnologylab.services.UIMAService;
import spark.ModelAndView;
import spark.Route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.texttechnologylab.routes.SearchApi.ActiveSearches;

public class CorpusUniverseApi {
    private ApplicationContext context = null;
    private UIMAService uimaService = null;
    private RAGService ragService = null;
    private PostgresqlDataInterface_Impl db = null;

    private Configuration freemakerConfig = Configuration.getDefaultConfiguration();

    public CorpusUniverseApi(ApplicationContext serviceContext, Configuration freemakerConfig) {
        this.uimaService = serviceContext.getBean(UIMAService.class);
        this.context = serviceContext;
        this.ragService = serviceContext.getBean(RAGService.class);
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.freemakerConfig = freemakerConfig;
    }

    public Route getCorpusUniverseView = ((request, response) -> {
        var model = new HashMap<String, Object>();
        try {
            model.put("universeId", UUID.randomUUID());
            // Later
        } catch (Exception ex) {
            // TODO: Logging
            model.put("data", "");
        }

        return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "corpus/corpusUniverse.ftl"));
    });

    public Route fromSearch = ((request, response) -> {
        var result = new HashMap<>();
        var gson = new Gson();

        var requestBody = gson.fromJson(request.body(), Map.class);
        var searchId = requestBody.get("searchId").toString();
        var level = UniverseLayer.valueOf(requestBody.get("level").toString());
        if (!ActiveSearches.containsKey(searchId)) {
            // TODO: Log here and return something? Dont know what yet
        }
        var activeSearchState = ActiveSearches.get(searchId);
        var biofidSearch = new Search_DefaultImpl();
        biofidSearch.fromSearchState(this.context, activeSearchState);
        var nodes = new ArrayList<CorpusUniverseNode>();

        switch (level){
            case DOCUMENTS:
                for(var doc: biofidSearch.getSearchHitsForPage(1).getCurrentDocuments()){
                    var docEmbedding = ragService.getDocumentEmbeddingOfDocument(doc.getId());
                    if(docEmbedding == null) continue;
                    var node = new CorpusUniverseNode();
                    node.setDocumentId(doc.getId());
                    if(doc.getDocumentTopicDistribution() != null) node.setPrimaryTopic(doc.getDocumentTopicDistribution().getYakeTopicOne());
                    node.setTsne2d(docEmbedding.getTsne2d());
                    node.setTsne3d(docEmbedding.getTsne3d());
                    nodes.add(node);
                }
        };

        result.put("nodes", nodes);
        result.put("level", level);

        return gson.toJson(result);
    });

}
