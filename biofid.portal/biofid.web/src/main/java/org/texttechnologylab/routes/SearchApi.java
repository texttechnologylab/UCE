package org.texttechnologylab.routes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import freemarker.template.Configuration;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.*;
import org.texttechnologylab.models.dto.SearchLayerDto;
import org.texttechnologylab.models.search.OrderByColumn;
import org.texttechnologylab.models.search.SearchLayer;
import org.texttechnologylab.models.search.SearchOrder;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.services.UIMAService;
import spark.ModelAndView;
import spark.Route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchApi {

    private ApplicationContext context = null;
    private UIMAService uimaService = null;
    private PostgresqlDataInterface_Impl db = null;
    private Configuration freemakerConfig = Configuration.getDefaultConfiguration();

    // TODO: outsource this to a db or something.
    private HashMap<String, SearchState> activeSearches;

    public SearchApi(ApplicationContext serviceContext, Configuration freemakerConfig) {
        this.freemakerConfig = freemakerConfig;
        this.context = serviceContext;
        this.uimaService = serviceContext.getBean(UIMAService.class);
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.activeSearches = new HashMap<String, SearchState>();
    }

    public Route activeSearchSort = ((request, response) -> {
        var searchId = request.queryParams("searchId");
        var order = request.queryParams("order").toUpperCase();
        var orderBy = request.queryParams("orderBy").toUpperCase();
        if (!activeSearches.containsKey(searchId)) {
            // TODO: Log here and return something? Dont know what yet
        }

        // Sort the current search state.
        var activeSearchState = activeSearches.get(searchId);
        activeSearchState.setOrder(SearchOrder.valueOf(order));
        activeSearchState.setOrderBy(OrderByColumn.valueOf(orderBy));
        var biofidSearch = new Search_DefaultImpl();
        biofidSearch.fromSearchState(this.context, activeSearchState);
        activeSearchState = biofidSearch.getSearchHitsForPage(activeSearchState.getCurrentPage());

        var model = new HashMap<String, Object>();
        model.put("searchState", activeSearchState);

        return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "search/components/documentList.ftl"));
    });

    public Route activeSearchPage = ((request, response) -> {
        var result = new HashMap<>();
        var searchId = request.queryParams("searchId");
        var page = Integer.parseInt(request.queryParams("page"));
        if (!activeSearches.containsKey(searchId)) {
            // TODO: Log here and return something? Dont know what yet
        }
        // Get the next pages.
        var activeSearchState = activeSearches.get(searchId);
        var biofidSearch = new Search_DefaultImpl();
        biofidSearch.fromSearchState(this.context, activeSearchState);
        activeSearchState = biofidSearch.getSearchHitsForPage(page);

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

        response.type("application/json");
        return gson.toJson(result);
    });

    public Route search = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        Map<String, Object> requestBody = gson.fromJson(request.body(), Map.class);
        var searchInput = requestBody.get("searchInput").toString();
        var corpusId = Long.parseLong(requestBody.get("corpusId").toString());
        // Parse the list of search layers into a list of search layer DTOs.
        var searchLayerDtos = (ArrayList<SearchLayerDto>) gson.fromJson(gson.toJson(requestBody.get("searchLayer")), new TypeToken<List<SearchLayerDto>>() { }.getType());

        // We have our own query language for SemanticRole Searches. Check if this is one of those.
        SearchState searchState = null;
        if (searchInput.startsWith("SR::")) {
            var semanticRoleSearch = new Search_SemanticRoleImpl(context, corpusId, searchInput);
            searchState = semanticRoleSearch.initSearch();
        } else {
            var corpusVm = db.getCorpusById(corpusId).getViewModel();
            // Define the search layers from the sent layers
            var searchLayers = searchLayerDtos
                    .stream()
                    .filter(SearchLayerDto::isChecked)
                    .map(dto -> SearchLayer.valueOf(dto.getName())).toList();
            var biofidSearch = new Search_DefaultImpl(context, searchInput, corpusId, searchLayers);
            searchState = biofidSearch.initSearch();
        }

        activeSearches.put(searchState.getSearchId().toString(), searchState);
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
        activeSearches.put(searchState.getSearchId().toString(), searchState);

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
