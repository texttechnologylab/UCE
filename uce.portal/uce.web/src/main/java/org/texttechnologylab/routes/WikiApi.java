package org.texttechnologylab.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.*;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.viewModels.wiki.CachedWikiPage;
import org.texttechnologylab.services.JenaSparqlService;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.services.WikiService;
import spark.ModelAndView;
import spark.Route;

import java.util.HashMap;
import java.util.Map;

public class WikiApi {

    private static final Logger logger = LogManager.getLogger();
    private Configuration freemarkerConfig;
    private JenaSparqlService jenaSparqlService;
    private WikiService wikiService;

    public WikiApi(ApplicationContext serviceContext, Configuration freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
        this.wikiService = serviceContext.getBean(WikiService.class);
        this.jenaSparqlService = serviceContext.getBean(JenaSparqlService.class);
    }

    public Route queryOntology = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        Map requestBody = gson.fromJson(request.body(), Map.class);

        try {
            var languageResources = LanguageResources.fromRequest(request);

            // TODO: Think about adding and handling the truplettype (sub, obj, pred) here.
            var tripletType = ExceptionUtils.tryCatchLog(() -> requestBody.get("tripletType").toString(),
                    (ex) -> logger.error("Need the type of the Triplet to query the sparql database.", ex));
            var value = ExceptionUtils.tryCatchLog(() -> requestBody.get("value").toString(),
                    (ex) -> logger.error("Need the value of the Triplet to execute the query.", ex));

            if (tripletType == null || tripletType.isEmpty() || value == null || value.isEmpty()) {
                model.put("information", languageResources.get("missingParameterError"));
                return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "defaultError.ftl"));
            }

            var nodes = jenaSparqlService.queryBySubject(value);
            // We don't need those children rdf nodes that are the exact same as the queried pne
            model.put("rdfNodes", nodes.stream().filter(n -> !n.getObject().getValue().equals(value)).toList());
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "/wiki/components/rdfNodeList.ftl"));
        } catch (Exception ex) {
            logger.error("Error querying the ontology in the graph database " +
                    "with id=" + request.attribute("id") + " to this endpoint for URI parameters.", ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

    public Route getAnnotationPage = ((request, response) -> {
        var model = new HashMap<String, Object>();

        try {
            var languageResources = LanguageResources.fromRequest(request);

            var wid = ExceptionUtils.tryCatchLog(() -> request.queryParams("wid"),
                    (ex) -> logger.error("The WikiView couldn't be generated - id missing.", ex));
            var coveredText = ExceptionUtils.tryCatchLog(() -> request.queryParams("covered"),
                    (ex) -> logger.error("The WikiView couldn't be generated - covered text missing.", ex));
            // Its actually fine if no additional params were given.
            var params = ExceptionUtils.tryCatchLog(() -> request.queryParams("params"), (ex) -> {
            });

            if (wid == null || !wid.contains("-") || coveredText == null || coveredText.isEmpty()) {
                model.put("information", languageResources.get("missingParameterError"));
                return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "defaultError.ftl"));
            }

            // Check if we have loaded, built and cached that wikipage before. We don't re-render it then.
            // BUT: We have different wiki views for different languages so the lang needs to be part of the key!
            var cacheId = wid + languageResources.getDefaultLanguage();
            if (SessionManager.CachedWikiPages.containsKey(cacheId)) {
                return SessionManager.CachedWikiPages.get(cacheId).getRenderedView();
            }

            // Determine the type. A wikiID always has the following format: <type>-<model_id>
            var splited = wid.split("-");
            var type = splited[0];
            var id = Long.parseLong(splited[1]);

            var renderView = "";
            if (type.startsWith("NE")) {
                // We then clicked onto a Named-Entity wiki item
                var viewModel = wikiService.buildNamedEntityWikiPageViewModel(id, coveredText);
                model.put("vm", viewModel);
                renderView = "/wiki/pages/namedEntityAnnotationPage.ftl";
            } else if (type.startsWith("TA")) {
                // We then clicked onto a Taxon wiki item
                var viewModel = wikiService.buildTaxonWikipageViewModel(id, coveredText);
                model.put("vm", viewModel);
                renderView = "/wiki/pages/taxonAnnotationPage.ftl";
            } else if (type.equals("TP") || type.equals("TD")) {
                // TP = TopicPage TD = TopicDocument
                var viewModel = wikiService.buildTopicAnnotationWikiPageViewModel(id, type, coveredText);
                model.put("vm", viewModel);
                renderView = "/wiki/pages/topicAnnotationPage.ftl";
            } else if (type.equals("D")) {
                // Then we have a document
                var viewModel = wikiService.buildDocumentWikiPageViewModel(id);
                model.put("vm", viewModel);
                renderView = "/wiki/pages/documentAnnotationPage.ftl";
            } else if (type.equals("L")) {
                // Then we have a lemma
                var viewModel = wikiService.buildLemmaAnnotationWikiPageViewModel(id, coveredText);
                model.put("vm", viewModel);
                renderView = "/wiki/pages/lemmaAnnotationPage.ftl";
            } else {
                // The type part of the wikiId was unknown. Throw an error.
                logger.warn("Someone tried to query a wiki page of a type that does not exist in UCE. This shouldn't happen.");
                model.put("information", languageResources.get("missingParameterError"));
                return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "defaultError.ftl"));
            }

            // cache and return the wiki page
            var view = new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, renderView));
            var cachedWikiPage = new CachedWikiPage(view);
            SessionManager.CachedWikiPages.put(cacheId, cachedWikiPage);
            return view;
        } catch (Exception ex) {
            logger.error("Error getting a wiki page for an annotation - best refer to the last logged API call " +
                    "with id=" + request.attribute("id") + " to this endpoint for URI parameters.", ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

}
