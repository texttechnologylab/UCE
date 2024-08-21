package org.texttechnologylab.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.*;
import org.texttechnologylab.models.search.OrderByColumn;
import org.texttechnologylab.models.search.SearchLayer;
import org.texttechnologylab.models.search.SearchOrder;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.services.RAGService;
import org.texttechnologylab.services.UIMAService;
import spark.ModelAndView;
import spark.Route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SearchApi {

    private ApplicationContext context = null;
    private UIMAService uimaService = null;
    private RAGService ragService = null;
    private PostgresqlDataInterface_Impl db = null;
    private Configuration freemakerConfig = Configuration.getDefaultConfiguration();

    // TODO: outsource this to a db or something.
    public static HashMap<String, SearchState> ActiveSearches = new HashMap<String, SearchState>();

    public SearchApi(ApplicationContext serviceContext, Configuration freemakerConfig) {
        this.freemakerConfig = freemakerConfig;
        this.context = serviceContext;
        this.uimaService = serviceContext.getBean(UIMAService.class);
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.ragService = serviceContext.getBean(RAGService.class);
    }

    public Route activeSearchSort = ((request, response) -> {
        var searchId = request.queryParams("searchId");
        var order = request.queryParams("order").toUpperCase();
        var orderBy = request.queryParams("orderBy").toUpperCase();
        if (!ActiveSearches.containsKey(searchId)) {
            // TODO: Log here and return something? Dont know what yet
        }

        // Sort the current search state.
        var activeSearchState = ActiveSearches.get(searchId);
        activeSearchState.setOrder(SearchOrder.valueOf(order));
        activeSearchState.setOrderBy(OrderByColumn.valueOf(orderBy));
        var search = new Search_DefaultImpl();
        search.fromSearchState(this.context, activeSearchState);
        activeSearchState = search.getSearchHitsForPage(activeSearchState.getCurrentPage());

        var model = new HashMap<String, Object>();
        model.put("searchState", activeSearchState);

        return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "search/components/documentList.ftl"));
    });

    public Route activeSearchPage = ((request, response) -> {
        var result = new HashMap<>();
        var searchId = request.queryParams("searchId");
        var page = Integer.parseInt(request.queryParams("page"));
        if (!ActiveSearches.containsKey(searchId)) {
            // TODO: Log here and return something? Dont know what yet
        }
        // Get the next pages.
        var activeSearchState = ActiveSearches.get(searchId);
        var search = new Search_DefaultImpl();
        search.fromSearchState(this.context, activeSearchState);
        activeSearchState = search.getSearchHitsForPage(page);

        var model = new HashMap<String, Object>();
        model.put("searchState", activeSearchState);

        // We return mutliple views:
        // the document view itself
        var documentsListView = new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "search/components/documentList.ftl"));
        result.put("documentsList", documentsListView);
        // The navigation changed
        var navigationView = new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "search/components/navigation.ftl"));
        result.put("navigationView", navigationView);
        var gson = new Gson();
        // And the keyword in context changed
        var keywordContext = new HashMap<String, Object>();
        keywordContext.put("contextState", activeSearchState.getKeywordInContextState());
        var keywordView = new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(keywordContext, "search/components/keywordInContext.ftl"));
        result.put("keywordInContextView", keywordView);

        response.type("application/json");
        return gson.toJson(result);
    });

    public Route search = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        Map<String, Object> requestBody = gson.fromJson(request.body(), Map.class);
        var searchInput = requestBody.get("searchInput").toString();
        var corpusId = Long.parseLong(requestBody.get("corpusId").toString());
        var metaOrNeLayer = requestBody.get("metaOrNeLayer").toString();
        var useEmbeddings = Boolean.parseBoolean(requestBody.get("useEmbeddings").toString());
        var includeKeywordInContext = Boolean.parseBoolean(requestBody.get("kwic").toString());

        // We have our own query language for SemanticRole Searches. Check if this is one of those.
        SearchState searchState = null;
        if (searchInput.startsWith("SR::")) {
            var semanticRoleSearch = new Search_SemanticRoleImpl(context, corpusId, searchInput);
            searchState = semanticRoleSearch.initSearch();
        } else {
            // Define the search layers from the sent layers
            var searchLayers = new ArrayList<SearchLayer>();

            if(metaOrNeLayer.equals("METADATA")) searchLayers.add(SearchLayer.METADATA);
            else searchLayers.add(SearchLayer.NAMED_ENTITIES);

            if(useEmbeddings) searchLayers.add(SearchLayer.EMBEDDINGS);
            if(includeKeywordInContext) searchLayers.add(SearchLayer.KEYWORDINCONTEXT);

            var search = new Search_DefaultImpl(context, searchInput, corpusId, searchLayers);
            searchState = search.initSearch();
        }

        ActiveSearches.put(searchState.getSearchId().toString(), searchState);
        model.put("searchState", searchState);

        return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "search/searchResult.ftl"));
    });

    public Route semanticRoleSearch = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        Map<String, Object> requestBody = gson.fromJson(request.body(), Map.class);

        var corpusId = Long.parseLong(requestBody.get("corpusId").toString());
        var arg0 = (ArrayList<String>) requestBody.get("arg0");
        var arg1 = (ArrayList<String>) requestBody.get("arg1");
        var argm = (ArrayList<String>) requestBody.get("argm");
        var verb = requestBody.get("verb").toString();

        var semanticRoleSearch = new Search_SemanticRoleImpl(context, corpusId, arg0, arg1, argm, verb);
        var searchState = semanticRoleSearch.initSearch();

        model.put("searchState", searchState);
        ActiveSearches.put(searchState.getSearchId().toString(), searchState);

        return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "search/searchResult.ftl"));
    });

    public Route getSemanticRoleBuilderView = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var corpusId = Long.parseLong(request.queryParams("corpusId"));

        var annotations = db.getAnnotationsOfCorpus(corpusId, 0, 250);
        model.put("time", annotations.stream().filter(a -> a.getInfo().equals("time")).toList());
        model.put("taxon", annotations.stream().filter(a -> a.getInfo().equals("taxon")).toList());
        model.put("organization", annotations.stream().filter(a -> a.getInfo().equals("ORGANIZATION")).toList());
        model.put("location", annotations.stream().filter(a -> a.getInfo().equals("LOCATION")).toList());
        model.put("person", annotations.stream().filter(a -> a.getInfo().equals("PERSON")).toList());
        model.put("misc", annotations.stream().filter(a -> a.getInfo().equals("MISC")).toList());

        return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "search/components/foundAnnotationsModal/foundAnnotationsModal.ftl"));
    });

}
