package org.texttechnologylab.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import org.apache.http.annotation.Obsolete;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.*;
import org.texttechnologylab.exceptions.ExceptionUtils;
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
    private static final Logger logger = LogManager.getLogger();
    private ApplicationContext context = null;
    private UIMAService uimaService = null;
    private RAGService ragService = null;
    private PostgresqlDataInterface_Impl db = null;
    private Configuration freemakerConfig = Configuration.getDefaultConfiguration();

    // TODO: outsource this to a db or something.
    // TODO^2: This needs to be adressed! At some point, if we cache the search states in RAM, we will overflow eventually!
    public static HashMap<String, SearchState> ActiveSearches = new HashMap<String, SearchState>();

    public SearchApi(ApplicationContext serviceContext, Configuration freemakerConfig) {
        this.freemakerConfig = freemakerConfig;
        this.context = serviceContext;
        this.uimaService = serviceContext.getBean(UIMAService.class);
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.ragService = serviceContext.getBean(RAGService.class);
    }

    public Route activeSearchSort = ((request, response) -> {
        var model = new HashMap<String, Object>();

        try {
            var languageResources = LanguageResources.fromRequest(request);
            var searchId = request.queryParams("searchId");
            var order = request.queryParams("order").toUpperCase();
            var orderBy = request.queryParams("orderBy").toUpperCase();
            if (!ActiveSearches.containsKey(searchId)) {
                logger.error("Issue fetching an active search state from the cache, id couldn't be found: " + searchId);
                return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(null, "defaultError.ftl"));
            }

            // Sort the current search state.
            var activeSearchState = ActiveSearches.get(searchId);
            activeSearchState.setOrder(SearchOrder.valueOf(order));
            activeSearchState.setOrderBy(OrderByColumn.valueOf(orderBy));
            var search = new Search_DefaultImpl();
            search.fromSearchState(this.context, languageResources.getDefaultLanguage(), activeSearchState);
            activeSearchState = search.getSearchHitsForPage(activeSearchState.getCurrentPage());

            model.put("searchState", activeSearchState);
        } catch (Exception ex) {
            logger.error("Error changing the sorting of an active search - best refer to the last logged API call " +
                    "with id=" + request.attribute("id") + " to this endpoint for URI parameters.", ex);
            return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
        return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "search/components/documentList.ftl"));
    });

    public Route activeSearchPage = ((request, response) -> {
        var result = new HashMap<>();
        result.put("status", 200);
        var gson = new Gson();

        try {
            var languageResources = LanguageResources.fromRequest(request);
            var searchId = request.queryParams("searchId");
            var page = Integer.parseInt(request.queryParams("page"));
            if (!ActiveSearches.containsKey(searchId)) {
                logger.error("Issue fetching an active search state from the cache, id couldn't be found: " + searchId);
                return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(null, "defaultError.ftl"));
            }

            // Get the next pages.
            var activeSearchState = ActiveSearches.get(searchId);
            var search = new Search_DefaultImpl();
            search.fromSearchState(this.context, languageResources.getDefaultLanguage(), activeSearchState);
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
            // And the keyword in context changed
            var keywordContext = new HashMap<String, Object>();
            keywordContext.put("contextState", activeSearchState.getKeywordInContextState());
            var keywordView = new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(keywordContext, "search/components/keywordInContext.ftl"));
            result.put("keywordInContextView", keywordView);
        } catch (Exception ex) {
            result.replace("status", 500);
            logger.error("Error changing the page of an active search - best refer to the last logged API call " +
                    "with id=" + request.attribute("id") + " to this endpoint for URI parameters.", ex);
        }

        response.type("application/json");
        return gson.toJson(result);
    });

    public Route search = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        Map<String, Object> requestBody = gson.fromJson(request.body(), Map.class);

        try {
            var languageResources = LanguageResources.fromRequest(request);
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

                if (metaOrNeLayer.equals("METADATA")) searchLayers.add(SearchLayer.METADATA);
                else searchLayers.add(SearchLayer.NAMED_ENTITIES);

                if (useEmbeddings) searchLayers.add(SearchLayer.EMBEDDINGS);
                if (includeKeywordInContext) searchLayers.add(SearchLayer.KEYWORDINCONTEXT);

                var search = new Search_DefaultImpl(context, searchInput, corpusId, languageResources.getDefaultLanguage(), searchLayers);
                searchState = search.initSearch();
            }

            ActiveSearches.put(searchState.getSearchId().toString(), searchState);
            model.put("searchState", searchState);

            return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "search/searchResult.ftl"));

        } catch (Exception ex) {
            logger.error("Error starting a new search with the request body:\n " + gson.toJson(requestBody), ex);
            return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

    /**
     * Old route that is currently not being used. The default search route checks what kind of search it is now.
     */
    @Obsolete
    public Route semanticRoleSearch = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        Map<String, Object> requestBody = gson.fromJson(request.body(), Map.class);

        try{
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
        } catch (Exception ex){
            logger.error("Error starting a new semantic role search with the request body:\n " + gson.toJson(requestBody), ex);
            return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

    public Route getSemanticRoleBuilderView = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var corpusId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("corpusId")),
                (ex) -> logger.error("Error: the url for the semantic role query builder requires a 'corpusId' query parameter. ", ex));
        if(corpusId == null) return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(null, "defaultError.ftl"));

        try{
            var annotations = db.getAnnotationsOfCorpus(corpusId, 0, 250);
            model.put("time", annotations.stream().filter(a -> a.getInfo().equals("time")).toList());
            model.put("taxon", annotations.stream().filter(a -> a.getInfo().equals("taxon")).toList());
            model.put("organization", annotations.stream().filter(a -> a.getInfo().equals("ORGANIZATION")).toList());
            model.put("location", annotations.stream().filter(a -> a.getInfo().equals("LOCATION")).toList());
            model.put("person", annotations.stream().filter(a -> a.getInfo().equals("PERSON")).toList());
            model.put("misc", annotations.stream().filter(a -> a.getInfo().equals("MISC")).toList());

            return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "search/components/foundAnnotationsModal/foundAnnotationsModal.ftl"));
        } catch (Exception ex){
            logger.error("Error getting the semantic role query builder view.", ex);
            return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

}
