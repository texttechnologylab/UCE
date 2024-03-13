package org.texttechnologylab.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.BiofidSearch;
import org.texttechnologylab.BiofidSearchState;
import org.texttechnologylab.models.search.SearchLayer;
import org.texttechnologylab.services.DatabaseService;
import org.texttechnologylab.services.UIMAService;
import spark.ModelAndView;
import spark.Route;
import spark.template.freemarker.FreeMarkerEngine;

import java.util.HashMap;
import java.util.Map;

public class SearchApi {

    private ApplicationContext context = null;
    private UIMAService uimaService = null;
    private DatabaseService db = null;
    private Configuration freemakerConfig = Configuration.getDefaultConfiguration();

    // TODO: outsource this to a db or something.
    private HashMap<String, BiofidSearchState> activeSearches;

    public SearchApi(ApplicationContext serviceContext, Configuration freemakerConfig) {
        this.freemakerConfig = freemakerConfig;
        this.context = serviceContext;
        this.uimaService = serviceContext.getBean(UIMAService.class);
        this.db = serviceContext.getBean(DatabaseService.class);
        this.activeSearches = new HashMap<String, BiofidSearchState>();
    }

    public Route activeSearchPage = ((request, response) -> {
        var result = new HashMap<>();
        var searchId = request.queryParams("searchId");
        var page = Integer.parseInt(request.queryParams("page"));
        if(!activeSearches.containsKey(searchId)){
            // TODO: Log here and return something? Dont know what yet
        }
        // Get the next pages.
        var activeSearchState = activeSearches.get(searchId);
        var biofidSearch = new BiofidSearch(this.context, activeSearchState);
        activeSearchState = biofidSearch.getSearchHitsForPage(page);

        var model = new HashMap<String, Object>();
        model.put("searchState", activeSearchState);

        // We return mutliple views:
        // the document view itself
        var documentsListView = new FreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "search/components/documentList.ftl"));
        result.put("documentsList", documentsListView);
        // The navigation changed
        var navigationView = new FreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "search/components/navigation.ftl"));
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

        var biofidSearch = new BiofidSearch(context, searchInput, new SearchLayer[]{
                SearchLayer.METADATA,
                SearchLayer.NAMED_ENTITIES
        });
        var searchState = biofidSearch.initSearch();

        //if (searchInput.equals("TEST")) {
        //    docs = this.db.searchForDocuments(0, 15);
        //}
        model.put("searchState", searchState);
        activeSearches.put(searchState.getSearchId().toString(), searchState);

        return new FreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "search/searchResult.ftl"));
    });
}
