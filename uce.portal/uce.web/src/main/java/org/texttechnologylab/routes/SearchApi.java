package org.texttechnologylab.routes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import freemarker.template.Configuration;
import org.apache.http.annotation.Obsolete;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.exception.SQLGrammarException;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.*;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.dto.LayeredSearchLayerDto;
import org.texttechnologylab.models.dto.UCEMetadataFilterDto;
import org.texttechnologylab.models.search.OrderByColumn;
import org.texttechnologylab.models.search.SearchLayer;
import org.texttechnologylab.models.search.SearchOrder;
import org.texttechnologylab.models.search.SearchType;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import spark.ModelAndView;
import spark.Route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SearchApi {
    private static final Logger logger = LogManager.getLogger();
    private ApplicationContext context = null;
    private PostgresqlDataInterface_Impl db = null;
    private Configuration freemarkerConfig;

    public SearchApi(ApplicationContext serviceContext, Configuration freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
        this.context = serviceContext;
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
    }

    public Route activeSearchSort = ((request, response) -> {
        var model = new HashMap<String, Object>();

        try {
            var languageResources = LanguageResources.fromRequest(request);
            var searchId = request.queryParams("searchId");
            var order = request.queryParams("order").toUpperCase();
            var orderBy = request.queryParams("orderBy").toUpperCase();
            if (!SessionManager.ActiveSearches.containsKey(searchId)) {
                logger.error("Issue fetching an active search state from the cache, id couldn't be found: " + searchId);
                model.put("information", languageResources.get("searchStateNotFound"));
                return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "defaultError.ftl"));
            }

            // Sort the current search state.
            var activeSearchState = (SearchState) SessionManager.ActiveSearches.get(searchId);
            activeSearchState.setOrder(SearchOrder.valueOf(order));
            activeSearchState.setOrderBy(OrderByColumn.valueOf(orderBy));
            Search search = new Search_DefaultImpl();
            if (activeSearchState.getSearchType() == SearchType.SEMANTICROLE) search = new Search_SemanticRoleImpl();
            search.fromSearchState(this.context, languageResources.getDefaultLanguage(), activeSearchState);
            activeSearchState = search.getSearchHitsForPage(activeSearchState.getCurrentPage());

            model.put("searchState", activeSearchState);
        } catch (Exception ex) {
            logger.error("Error changing the sorting of an active search - best refer to the last logged API call " +
                    "with id=" + request.attribute("id") + " to this endpoint for URI parameters.", ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
        return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "search/components/documentList.ftl"));
    });

    public Route activeSearchPage = ((request, response) -> {
        var result = new HashMap<>();
        result.put("status", 200);
        var gson = new Gson();

        try {
            var languageResources = LanguageResources.fromRequest(request);
            var searchId = request.queryParams("searchId");
            var page = Integer.parseInt(request.queryParams("page"));
            if (!SessionManager.ActiveSearches.containsKey(searchId)) {
                logger.error("Issue fetching an active search state from the cache, id couldn't be found: " + searchId);
                var model = new HashMap<String, Object>();
                model.put("information", languageResources.get("searchStateNotFound"));
                return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "defaultError.ftl"));
            }

            // Get the next pages.
            var activeSearchState = (SearchState) SessionManager.ActiveSearches.get(searchId);
            Search search = new Search_DefaultImpl();
            if (activeSearchState.getSearchType() == SearchType.SEMANTICROLE) search = new Search_SemanticRoleImpl();
            search.fromSearchState(this.context, languageResources.getDefaultLanguage(), activeSearchState);
            activeSearchState = search.getSearchHitsForPage(page);

            var model = new HashMap<String, Object>();
            model.put("searchState", activeSearchState);

            // We return mutliple views:
            // the document view itself
            var documentsListView = new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "search/components/documentList.ftl"));
            result.put("documentsList", documentsListView);
            // The navigation changed
            var navigationView = new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "search/components/navigation.ftl"));
            result.put("navigationView", navigationView);
            // And the keyword in context changed
            var keywordContext = new HashMap<String, Object>();
            keywordContext.put("contextState", activeSearchState.getKeywordInContextState());
            var keywordView = new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(keywordContext, "search/components/keywordInContext.ftl"));
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
        var requestBody = gson.fromJson(request.body(), Map.class);
        var languageResources = LanguageResources.fromRequest(request);

        try {
            var searchInput = requestBody.get("searchInput").toString();
            var corpusId = Long.parseLong(requestBody.get("corpusId").toString());
            model.put("corpusVm", db.getCorpusById(corpusId).getViewModel());
            var fulltextOrNeLayer = requestBody.get("fulltextOrNeLayer").toString();
            var useEmbeddings = Boolean.parseBoolean(requestBody.get("useEmbeddings").toString());
            var includeKeywordInContext = Boolean.parseBoolean(requestBody.get("kwic").toString());
            var enrichSearchTerm = Boolean.parseBoolean(requestBody.get("enrich").toString());
            var proModeActivated = Boolean.parseBoolean(requestBody.get("proMode").toString());
            var layeredSearchId = requestBody.get("layeredSearchId").toString();
            var layers = new ArrayList<LayeredSearchLayerDto>();

            // It's not tragic if no filters are given, not every corpus has them.
            ArrayList<UCEMetadataFilterDto> uceMetadataFilters = ExceptionUtils.tryCatchLog(
                    () -> gson.fromJson(
                            requestBody.get("uceMetadataFilters").toString(),
                            new TypeToken<ArrayList<UCEMetadataFilterDto>>() {
                            }.getType()),
                    (ex) -> {
                    });

            LayeredSearch layeredSearch = null;
            // If the layeredSearchId isn't empty, we need to apply the layered search as well.
            if (!layeredSearchId.isEmpty()) {
                layeredSearch = (LayeredSearch) SessionManager.ActiveLayeredSearches.get(layeredSearchId);
                if(layeredSearch != null){
                    layers = gson.fromJson(
                            requestBody.get("layers").toString(),
                            new TypeToken<ArrayList<LayeredSearchLayerDto>>() {
                            }.getType());
                    layeredSearch.updateLayers(layers);
                }
            }

            // We have our own query language for SemanticRole Searches. Check if this is one of those.
            SearchState searchState = null;
            if (searchInput.startsWith("SR::")) {
                var semanticRoleSearch = new Search_SemanticRoleImpl(context, corpusId, searchInput);
                searchState = semanticRoleSearch.initSearch();
            } else {
                // Define the search layers from the sent layers
                var searchLayers = new ArrayList<SearchLayer>();

                if (fulltextOrNeLayer.equals("FULLTEXT")) searchLayers.add(SearchLayer.FULLTEXT);
                else searchLayers.add(SearchLayer.NAMED_ENTITIES);

                if (useEmbeddings) searchLayers.add(SearchLayer.EMBEDDINGS);
                if (includeKeywordInContext) searchLayers.add(SearchLayer.KEYWORDINCONTEXT);

                var search = new Search_DefaultImpl(
                        context,
                        searchInput,
                        corpusId,
                        languageResources.getDefaultLanguage(),
                        searchLayers,
                        enrichSearchTerm,
                        proModeActivated)
                        .withUceMetadataFilters(uceMetadataFilters)
                        .withLayeredSearch(layeredSearch);

                searchState = search.initSearch();
            }

            SessionManager.ActiveSearches.put(searchState.getSearchId().toString(), searchState);
            model.put("searchState", searchState);

            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "search/searchResult.ftl"));
        } catch (SQLGrammarException grammarException) {
            response.status(406);
            return languageResources.get("searchGrammarError");
        } catch (Exception ex) {
            logger.error("Error starting a new search with the request body:\n " + gson.toJson(requestBody), ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

    public Route layeredSearch = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        var requestBody = gson.fromJson(request.body(), Map.class);
        var languageResources = LanguageResources.fromRequest(request);

        try {
            ArrayList<LayeredSearchLayerDto> layers = gson.fromJson(
                    requestBody.get("layers").toString(),
                    new TypeToken<ArrayList<LayeredSearchLayerDto>>() {
                    }.getType());
            var searchId = requestBody.get("searchId").toString();

            // If there isn't an existing searchId, we create a new layeredSearch and cache it
            LayeredSearch layeredSearch = null;
            if(SessionManager.ActiveLayeredSearches.containsKey(searchId)){
                layeredSearch = (LayeredSearch) SessionManager.ActiveLayeredSearches.get(searchId);
            } else{
                layeredSearch = new LayeredSearch(this.context, searchId);
                layeredSearch.init();
                SessionManager.ActiveLayeredSearches.put(layeredSearch.getId(), layeredSearch);
            }

            // Either way, update the layers
            layeredSearch.updateLayers(layers);
            return gson.toJson(layeredSearch.getLayers());
        } catch (Exception ex) {
            logger.error("Error starting a new layered search with the request body:\n " + gson.toJson(requestBody), ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
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

        try {
            var corpusId = Long.parseLong(requestBody.get("corpusId").toString());
            model.put("corpusVm", db.getCorpusById(corpusId).getViewModel());
            var arg0 = (ArrayList<String>) requestBody.get("arg0");
            var arg1 = (ArrayList<String>) requestBody.get("arg1");
            var argm = (ArrayList<String>) requestBody.get("argm");
            var verb = requestBody.get("verb").toString();

            var semanticRoleSearch = new Search_SemanticRoleImpl(context, corpusId, arg0, arg1, argm, verb);
            var searchState = semanticRoleSearch.initSearch();

            model.put("searchState", searchState);
            SessionManager.ActiveSearches.put(searchState.getSearchId().toString(), searchState);

            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "search/searchResult.ftl"));
        } catch (Exception ex) {
            logger.error("Error starting a new semantic role search with the request body:\n " + gson.toJson(requestBody), ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

    public Route getSemanticRoleBuilderView = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var corpusId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("corpusId")),
                (ex) -> logger.error("Error: the url for the semantic role query builder requires a 'corpusId' query parameter. ", ex));
        if (corpusId == null)
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));

        try {
            var annotations = db.getAnnotationsOfCorpus(corpusId, 0, 250);
            model.put("time", annotations.stream().filter(a -> a.getInfo().equals("time")).toList());
            model.put("taxon", annotations.stream().filter(a -> a.getInfo().equals("taxon")).toList());
            model.put("organization", annotations.stream().filter(a -> a.getInfo().equals("ORGANIZATION")).toList());
            model.put("location", annotations.stream().filter(a -> a.getInfo().equals("LOCATION")).toList());
            model.put("person", annotations.stream().filter(a -> a.getInfo().equals("PERSON")).toList());
            model.put("misc", annotations.stream().filter(a -> a.getInfo().equals("MISC")).toList());

            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "search/components/foundAnnotationsModal/foundAnnotationsModal.ftl"));
        } catch (Exception ex) {
            logger.error("Error getting the semantic role query builder view.", ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

}
