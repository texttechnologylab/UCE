package org.texttechnologylab.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.*;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.services.WikiService;
import spark.ModelAndView;
import spark.Route;

import java.util.HashMap;

public class WikiApi {

    private static final Logger logger = LogManager.getLogger();
    private ApplicationContext context = null;
    private PostgresqlDataInterface_Impl db = null;
    private Configuration freemakerConfig = Configuration.getDefaultConfiguration();
    private WikiService wikiService = null;

    public WikiApi(ApplicationContext serviceContext, Configuration freemakerConfig) {
        this.freemakerConfig = freemakerConfig;
        this.context = serviceContext;
        this.wikiService = serviceContext.getBean(WikiService.class);
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
    }

    public Route getAnnotationPage = ((request, response) -> {
        var gson = new Gson();
        var model = new HashMap<String, Object>();

        try {
            var languageResources = LanguageResources.fromRequest(request);

            var id = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("id")),
                    (ex) -> logger.error("The WikiView couldn't be generated - id missing.", ex));
            var type = ExceptionUtils.tryCatchLog(() -> request.queryParams("type"),
                    (ex) -> logger.error("The WikiView couldn't be generated - type missing.", ex));
            var coveredText = ExceptionUtils.tryCatchLog(() -> request.queryParams("covered"),
                    (ex) -> logger.error("The WikiView couldn't be generated - covered text missing.", ex));

            if (id == null || type == null || type.isEmpty() || coveredText == null || coveredText.isEmpty()) {
                model.put("information", languageResources.get("missingParameterError"));
                return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "defaultError.ftl"));
            }

            if(type.equals("NE")){
                // We generate a NER annotation view
                var xd = "";
            } else if(type.contains("TOPIC")){
                var viewModel = wikiService.buildTopicAnnotationWikiPageViewModel(id, type, coveredText);
                model.put("vm", viewModel);
                return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "/wiki/pages/topicAnnotationPage.ftl"));
            }

        } catch (Exception ex) {
            logger.error("Error getting a wiki page for an annotation - best refer to the last logged API call " +
                    "with id=" + request.attribute("id") + " to this endpoint for URI parameters.", ex);
            return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }

        return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "/wiki/pages/annotationPage.ftl"));
    });

}
