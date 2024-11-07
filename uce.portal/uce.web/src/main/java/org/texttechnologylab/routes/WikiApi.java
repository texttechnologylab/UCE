package org.texttechnologylab.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.*;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.corpus.DocumentTopicDistribution;
import org.texttechnologylab.models.corpus.PageTopicDistribution;
import org.texttechnologylab.models.corpus.TopicDistribution;
import org.texttechnologylab.models.search.SearchType;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.services.RAGService;
import org.texttechnologylab.services.UIMAService;
import org.texttechnologylab.viewModels.wiki.TopicAnnotationWikiPageViewModel;
import spark.ModelAndView;
import spark.Route;

import java.util.HashMap;

public class WikiApi {

    private static final Logger logger = LogManager.getLogger();
    private ApplicationContext context = null;
    private PostgresqlDataInterface_Impl db = null;
    private Configuration freemakerConfig = Configuration.getDefaultConfiguration();

    public WikiApi(ApplicationContext serviceContext, Configuration freemakerConfig) {
        this.freemakerConfig = freemakerConfig;
        this.context = serviceContext;
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

            if (id == null || type == null || type.isEmpty()) {
                model.put("information", languageResources.get("missingParameterError"));
                return new CustomFreeMarkerEngine(this.freemakerConfig).render(new ModelAndView(model, "defaultError.ftl"));
            }

            if(type.equals("NE")){
                // We generate a NER annotation view
                var xd = "";
            } else if(type.contains("TOPIC")){
                // We generate a View Model for a clicked topic.
                var viewModel = new TopicAnnotationWikiPageViewModel();
                viewModel.setType(type.substring(0, 1));
                if(type.startsWith("D")) {
                    var docDist = db.getTopicDistributionById(DocumentTopicDistribution.class, id);
                    viewModel.setTopicDistribution(docDist);
                    viewModel.setDocument(docDist.getDocument());
                } else if(type.startsWith("P")){
                    var pageDist = db.getTopicDistributionById(PageTopicDistribution.class, id);
                    viewModel.setTopicDistribution(pageDist);
                    viewModel.setPage(pageDist.getPage());
                    viewModel.setDocument(db.getDocumentById(pageDist.getPage().getDocumentId()));
                }
                viewModel.setCorpus(db.getCorpusById(viewModel.getDocument().getCorpusId()));

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
