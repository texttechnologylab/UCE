package org.texttechnologylab.uce.web.routes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import freemarker.template.Configuration;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.exception.SQLGrammarException;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.models.authentication.UceUser;
import org.texttechnologylab.uce.common.models.dto.LayeredSearchLayerDto;
import org.texttechnologylab.uce.common.models.dto.UCEMetadataFilterDto;
import org.texttechnologylab.uce.common.models.search.OrderByColumn;
import org.texttechnologylab.uce.common.models.search.SearchLayer;
import org.texttechnologylab.uce.common.models.search.SearchOrder;
import org.texttechnologylab.uce.common.models.search.SearchType;
import org.texttechnologylab.uce.common.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.uce.search.*;
import org.texttechnologylab.uce.web.CustomFreeMarkerEngine;
import org.texttechnologylab.uce.web.LanguageResources;
import org.texttechnologylab.uce.web.SessionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SearchApi implements UceApi {
    private static final Logger logger = LogManager.getLogger(SearchApi.class);
    private ApplicationContext context = null;
    private PostgresqlDataInterface_Impl db = null;
    private Configuration freemarkerConfig;

    public SearchApi(ApplicationContext serviceContext, Configuration freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
        this.context = serviceContext;
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
    }

    public void activeSearchSort(Context ctx) {
        var model = new HashMap<String, Object>();

        try {
            var languageResources = LanguageResources.fromRequest(ctx);
            var searchId = ctx.queryParam("searchId");
            var order = ctx.queryParam("order").toUpperCase();
            var orderBy = ctx.queryParam("orderBy").toUpperCase();
            if (!SessionManager.ActiveSearches.containsKey(searchId)) {
                logger.error("Issue fetching an active search state from the cache, id couldn't be found: " + searchId);
                model.put("information", languageResources.get("searchStateNotFound"));
                ctx.render("defaultError.ftl", model);
                return;
            }

            UceUser user = ctx.sessionAttribute("uceUser");

            // Sort the current search state.
            var activeSearchState = (SearchState) SessionManager.ActiveSearches.get(searchId);
            activeSearchState.setOrder(SearchOrder.valueOf(order));
            activeSearchState.setOrderBy(OrderByColumn.valueOf(orderBy));
            Search search = new Search_DefaultImpl();
            if (activeSearchState.getSearchType() == SearchType.SEMANTICROLE) {
                search = new Search_SemanticRoleImpl();
            } else if (activeSearchState.getSearchType() == SearchType.NEG) {
                search = new SearchCompleteNegation();
            }
            search.fromSearchState(this.context, languageResources.getDefaultLanguage(), activeSearchState);
            activeSearchState = search.getSearchHitsForPage(activeSearchState.getCurrentPage(), user);

            model.put("searchState", activeSearchState);
        } catch (Exception ex) {
            logger.error("Error changing the sorting of an active search - best refer to the last logged API call " +
                    "with id=" + ctx.attribute("id") + " to this endpoint for URI parameters.", ex);
            ctx.render("defaultError.ftl");
            return;
        }
        ctx.render("search/components/documentList.ftl", model);
    }

    public void activeSearchPage(Context ctx) {
        var result = new HashMap<>();
        result.put("status", 200);
        var gson = new Gson();

        try {
            var languageResources = LanguageResources.fromRequest(ctx);
            var searchId = ctx.queryParam("searchId");
            var page = Integer.parseInt(ctx.queryParam("page"));
            if (!SessionManager.ActiveSearches.containsKey(searchId)) {
                logger.error("Issue fetching an active search state from the cache, id couldn't be found: " + searchId);
                var model = new HashMap<String, Object>();
                model.put("information", languageResources.get("searchStateNotFound"));
                ctx.render("defaultError.ftl", model);
                return;
            }

            UceUser user = ctx.sessionAttribute("uceUser");

            // Get the next pages.
            var activeSearchState = (SearchState) SessionManager.ActiveSearches.get(searchId);
            Search search = new Search_DefaultImpl();
            if (activeSearchState.getSearchType() == SearchType.SEMANTICROLE) {
                search = new Search_SemanticRoleImpl();
            } else if (activeSearchState.getSearchType() == SearchType.NEG) {
                search = new SearchCompleteNegation();
            }
            search.fromSearchState(this.context, languageResources.getDefaultLanguage(), activeSearchState);
            activeSearchState = search.getSearchHitsForPage(page, user);

            var model = new HashMap<String, Object>();
            model.put("searchState", activeSearchState);

            // We return mutliple views:
            // the document view itself
            var documentsListView = new CustomFreeMarkerEngine(this.freemarkerConfig).render("search/components/documentList.ftl", model, ctx);
            result.put("documentsList", documentsListView);
            // The navigation changed
            var navigationView = new CustomFreeMarkerEngine(this.freemarkerConfig).render("search/components/navigation.ftl", model, ctx);
            result.put("navigationView", navigationView);
            // And the keyword in context changed
            var keywordContext = new HashMap<String, Object>();
            keywordContext.put("contextState", activeSearchState.getKeywordInContextState());
            var keywordView = new CustomFreeMarkerEngine(this.freemarkerConfig).render("search/components/keywordInContext.ftl", keywordContext, ctx);
            result.put("keywordInContextView", keywordView);
            // And finally, the visualization data JSON
            result.put("searchVisualization", activeSearchState.getVisualizationData());
        } catch (Exception ex) {
            result.replace("status", 500);
            logger.error("Error changing the page of an active search - best refer to the last logged API call " +
                    "with id=" + ctx.attribute("id") + " to this endpoint for URI parameters.", ex);
        }

        ctx.json(result);
    }

    public void search(Context ctx) throws IOException {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        var requestBody = gson.fromJson(ctx.body(), Map.class);
        var languageResources = LanguageResources.fromRequest(ctx);

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
            UceUser user = ctx.sessionAttribute("uceUser");

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
                searchState = semanticRoleSearch.initSearch(user);
            } else if (searchInput.startsWith("NEG::")) {
                var negRoleSearch = new SearchCompleteNegation(
                        context,
                        corpusId,
                        searchInput)
                        .withUceMetadataFilters(uceMetadataFilters);
                searchState = negRoleSearch.initSearch(user);
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

                searchState = search.initSearch(user);
            }

            SessionManager.ActiveSearches.put(searchState.getSearchId().toString(), searchState);
            model.put("searchState", searchState);

            ctx.render("search/searchResult.ftl", model);
        } catch (SQLGrammarException grammarException) {
            ctx.status(406);
            ctx.result(languageResources.get("searchGrammarError"));
        } catch (Exception ex) {
            logger.error("Error starting a new search with the request body:\n " + gson.toJson(requestBody), ex);
            ctx.render("defaultError.ftl");
        }
    }

    public void layeredSearch(Context ctx) throws IOException {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        var requestBody = gson.fromJson(ctx.body(), Map.class);
        var languageResources = LanguageResources.fromRequest(ctx);

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
            ctx.json(layeredSearch.getLayers());
        } catch (Exception ex) {
            logger.error("Error starting a new layered search with the request body:\n " + gson.toJson(requestBody), ex);
            ctx.status(500);
            ctx.render("defaultError.ftl");
        }
    }

    /**
     * Old route that is currently not being used. The default search route checks what kind of search it is now.
     */
    public void semanticRoleSearch(Context ctx) {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        Map<String, Object> requestBody = gson.fromJson(ctx.body(), Map.class);

        try {
            var corpusId = Long.parseLong(requestBody.get("corpusId").toString());
            model.put("corpusVm", db.getCorpusById(corpusId).getViewModel());
            var arg0 = (ArrayList<String>) requestBody.get("arg0");
            var arg1 = (ArrayList<String>) requestBody.get("arg1");
            var argm = (ArrayList<String>) requestBody.get("argm");
            var verb = requestBody.get("verb").toString();
            UceUser user = ctx.sessionAttribute("uceUser");

            var semanticRoleSearch = new Search_SemanticRoleImpl(context, corpusId, arg0, arg1, argm, verb);
            var searchState = semanticRoleSearch.initSearch(user);

            model.put("searchState", searchState);
            SessionManager.ActiveSearches.put(searchState.getSearchId().toString(), searchState);

            ctx.render("search/searchResult.ftl", model);
        } catch (Exception ex) {
            logger.error("Error starting a new semantic role search with the request body:\n " + gson.toJson(requestBody), ex);
            ctx.render("defaultError.ftl");
        }
    }

    public void getSemanticRoleBuilderView(Context ctx) {
        var model = new HashMap<String, Object>();
        var corpusId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(ctx.queryParam("corpusId")),
                (ex) -> logger.error("Error: the url for the semantic role query builder requires a 'corpusId' query parameter. ", ex));
        if (corpusId == null) {
            ctx.render("defaultError.ftl");
            return;
        }

        try {
            var annotations = db.getAnnotationsOfCorpus(corpusId, 0, 250);
            model.put("time", annotations.stream().filter(a -> a.getInfo().equals("time")).toList());
            model.put("taxon", annotations.stream().filter(a -> a.getInfo().equals("taxon")).toList());
            model.put("organization", annotations.stream().filter(a -> a.getInfo().equals("ORGANIZATION")).toList());
            model.put("location", annotations.stream().filter(a -> a.getInfo().equals("LOCATION")).toList());
            model.put("person", annotations.stream().filter(a -> a.getInfo().equals("PERSON")).toList());
            model.put("misc", annotations.stream().filter(a -> a.getInfo().equals("MISC")).toList());

            ctx.render("search/components/foundAnnotationsModal/foundAnnotationsModal.ftl", model);
        } catch (Exception ex) {
            logger.error("Error getting the semantic role query builder view.", ex);
            ctx.render("defaultError.ftl");
        }
    }

}
