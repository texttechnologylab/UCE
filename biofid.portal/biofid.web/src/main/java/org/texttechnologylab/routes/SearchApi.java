package org.texttechnologylab.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.BiofidSearch;
import org.texttechnologylab.BiofidSearchLayer;
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

    public SearchApi(ApplicationContext serviceContext, Configuration freemakerConfig) {
        this.freemakerConfig = freemakerConfig;
        this.context = serviceContext;
        this.uimaService = serviceContext.getBean(UIMAService.class);
        this.db = serviceContext.getBean(DatabaseService.class);
    }

    public Route search = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        Map<String, Object> requestBody = gson.fromJson(request.body(), Map.class);
        var searchInput = requestBody.get("searchInput").toString();

        var biofidSearch = new BiofidSearch(context, searchInput, new BiofidSearchLayer[]{
                BiofidSearchLayer.METADATA,
                BiofidSearchLayer.NAMED_ENTITIES
        });
        var docs = biofidSearch.initSearch();

        if (searchInput.equals("TEST")) {
            docs = this.db.searchForDocuments(0, 15);
        }
        model.put("documents", docs);

        return new FreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "search/searchResult.ftl"));
    });
}
