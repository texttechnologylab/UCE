package org.texttechnologylab.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.*;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.freeMarker.Renderer;
import org.texttechnologylab.models.dto.LinkableNodeDto;
import org.texttechnologylab.models.viewModels.wiki.CachedWikiPage;
import org.texttechnologylab.services.*;
import org.texttechnologylab.utils.SystemStatus;
import spark.ModelAndView;
import spark.Route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WikiApi {

    private static final Logger logger = LogManager.getLogger(WikiApi.class);
    private LexiconService lexiconService;
    private Configuration freemarkerConfig;
    private JenaSparqlService jenaSparqlService;
    private WikiService wikiService;
    private PostgresqlDataInterface_Impl db;
    private final Gson gson = new Gson();

    public WikiApi(ApplicationContext serviceContext, Configuration freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.lexiconService = serviceContext.getBean(LexiconService.class);
        this.wikiService = serviceContext.getBean(WikiService.class);
        this.jenaSparqlService = serviceContext.getBean(JenaSparqlService.class);
    }

    public Route queryOntology = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        var requestBody = gson.fromJson(request.body(), Map.class);

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

    public Route getPage = ((request, response) -> {
        var model = new HashMap<String, Object>();

        try {
            var languageResources = LanguageResources.fromRequest(request);

            var wid = ExceptionUtils.tryCatchLog(() -> request.queryParams("wid"),
                    (ex) -> logger.error("The WikiView couldn't be generated - id missing.", ex));
            var coveredText = ExceptionUtils.tryCatchLog(() -> request.queryParams("covered"),
                    (ex) -> logger.error("The WikiView couldn't be generated - covered text missing.", ex));
            // It's actually fine if no additional params were given. I'm not sure anymore I'll use them anyways.
            var params = ExceptionUtils.tryCatchLog(() -> request.queryParams("params"), (ex) -> {
            });

            // TODO: logging
            if (wid == null || !wid.contains("-") || coveredText == null || coveredText.isEmpty()) {
                model.put("information", languageResources.get("missingParameterError"));
                return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "defaultError.ftl"));
            }

            // Determine the type. A wikiID always has the following format: <type>-<model_id>
            var split = wid.split("-");
            var type = split[0];

            // Check if we have loaded, built and cached that wiki page before. We don't re-render it then.
            // BUT: We have different wiki views for different languages so the lang needs to be part of the key!
            var cacheId = wid + languageResources.getDefaultLanguage();

            if (type.startsWith("DTR")) {
                cacheId += coveredText;
            }

            if (SessionManager.CachedWikiPages.containsKey(cacheId)) {
                return ((CachedWikiPage)SessionManager.CachedWikiPages.get(cacheId)).getRenderedView();
            }

            // A missing id isn't necessarily bad, as we also have documentation pages etc.
            var id = ExceptionUtils.tryCatchLog(() -> Long.parseLong(split[1]), (ex) -> {});

            var renderView = "";
            if(type.startsWith("DOC")){
                model.put("jenaSparqlAlive", SystemStatus.JenaSparqlStatus.isAlive());
                if(split[1].equals("SEARCH")){
                    model.put("vm", wikiService.buildDocumentationWikiPageViewModel());
                    renderView = "/wiki/pages/searchDocumentation.ftl";
                }
            } else if (type.startsWith("NE")) {
                // We then clicked onto a Named-Entity wiki item
                model.put("vm", wikiService.buildNamedEntityWikiPageViewModel(id, coveredText));
                renderView = "/wiki/pages/namedEntityAnnotationPage.ftl";
            } else if (type.startsWith("TA")) {
                // We then clicked onto a Taxon wiki item
                model.put("vm", wikiService.buildTaxonWikipageViewModel(id, coveredText));
                renderView = "/wiki/pages/taxonAnnotationPage.ftl";
            } else if (type.equals("TP") || type.equals("TD")) {
                // TP = TopicPage TD = TopicDocument
                model.put("vm", wikiService.buildTopicAnnotationWikiPageViewModel(id, type, coveredText));
                renderView = "/wiki/pages/topicAnnotationPage.ftl";
            } else if (type.equals("TI")) {
                // Then we have a Time annotation
                model.put("vm", wikiService.buildTimeAnnotationWikiPageViewModel(id, coveredText));
                renderView = "/wiki/pages/timeAnnotationPage.ftl";
            } else if (type.equals("C")) {
                // Then we have a corpus
                model.put("vm", wikiService.buildCorpusWikiPageViewModle(id, coveredText));
                renderView = "/wiki/pages/corpusPage.ftl";
            } else if (type.equals("D")) {
                // Then we have a document
                model.put("vm", wikiService.buildDocumentWikiPageViewModel(id));
                renderView = "/wiki/pages/documentAnnotationPage.ftl";
            } else if (type.equals("L")) {
                // Then we have a lemma
                model.put("vm", wikiService.buildLemmaAnnotationWikiPageViewModel(id, coveredText));
                renderView = "/wiki/pages/lemmaAnnotationPage.ftl";
            } else if (type.startsWith("CU")) {
                model.put("vm", wikiService.buildNegationAnnotationWikiPageViewModel(id, coveredText));
                renderView = "/wiki/pages/negationAnnotationPage.ftl";
            } else if (type.startsWith("UT")) {
                model.put("vm", wikiService.buildUnifiedTopicWikiPageViewModel(id, coveredText));
                renderView = "/wiki/pages/unifiedTopicAnnotationPage.ftl";
            } else if (type.startsWith("DTR")) {
                model.put("vm", wikiService.buildTopicWikiPageViewModel(id, coveredText));
                renderView = "/wiki/pages/topicPage.ftl";
           }
            else {
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
            logger.error("Error getting a wiki page - best refer to the last logged API call " +
                    "with id=" + request.attribute("id") + " to this endpoint for URI parameters.", ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

    public Route getOccurrencesOfLexiconEntry = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var languageResources = LanguageResources.fromRequest(request);
        var requestBody = gson.fromJson(request.body(), Map.class);

        var coveredText = ExceptionUtils.tryCatchLog(() -> requestBody.get("coveredText").toString(),
                (ex) -> logger.error("Couldn't fetch occurrences of lexicon entry - coveredText missing.", ex));
        var type = ExceptionUtils.tryCatchLog(() -> requestBody.get("type").toString(),
                (ex) -> logger.error("Couldn't fetch occurrences of lexicon entry - type missing.", ex));
        var skip = ExceptionUtils.tryCatchLog(() -> (int)Double.parseDouble(requestBody.get("skip").toString()),
                (ex) -> logger.error("Calling a lexicon entry without skip shouldn't happen.", ex));
        var take = ExceptionUtils.tryCatchLog(() -> (int)Double.parseDouble(requestBody.get("take").toString()),
                (ex) -> logger.error("Calling a lexicon entry without take shouldn't happen.", ex));
        if(coveredText == null || type == null || skip == null || take == null){
            model.put("information", languageResources.get("missingParameterError"));
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "defaultError.ftl"));
        }

        try {
            var occurrences = ExceptionUtils.tryCatchLog(
                    () -> lexiconService.getOccurrenceViewModelsOfEntry(coveredText, type, skip, take),
                    (ex) -> logger.error("Error fetching lexicon entries: ", ex));
            if(occurrences == null){
                response.status(500);
                return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
            }

            model.put("occurrences", occurrences);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "/wiki/lexicon/occurrencesList.ftl"));
        } catch (Exception ex) {
            logger.error("Error getting occurrences from a lexicon entry - best refer to the last logged API call " +
                    "with id=" + request.attribute("id") + " to this endpoint for URI parameters.", ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

    public Route getLexicon = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var requestBody = gson.fromJson(request.body(), Map.class);
        var languageResources = LanguageResources.fromRequest(request);

        var skip = ExceptionUtils.tryCatchLog(() -> (int)Double.parseDouble(requestBody.get("skip").toString()),
                (ex) -> logger.error("Calling a lexicon without skip shouldn't happen.", ex));
        var take = ExceptionUtils.tryCatchLog(() -> (int)Double.parseDouble(requestBody.get("take").toString()),
                (ex) -> logger.error("Calling a lexicon without take shouldn't happen.", ex));
        if(skip == null || take == null){
            model.put("information", languageResources.get("missingParameterError"));
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "defaultError.ftl"));
        }

        // These parameters are optional. We can work without just fine.
        var alphabet = ExceptionUtils.tryCatchLog(() -> (List<String>) requestBody.get("alphabet"), (ex) -> {});
        var annotationFilters = ExceptionUtils.tryCatchLog(() -> (List<String>) requestBody.get("annotationFilters"), (ex) -> {});
        var sortColumn = ExceptionUtils.tryCatchLog(() -> requestBody.get("sortColumn").toString(), (ex) -> {});
        var sortDirection = ExceptionUtils.tryCatchLog(() -> requestBody.get("sortDirection").toString(), (ex) -> {});
        var searchInput = ExceptionUtils.tryCatchLog(() -> requestBody.get("searchInput").toString(), (ex) -> {});

        try {
            var entries = ExceptionUtils.tryCatchLog(
                    () -> lexiconService.getEntries(skip, take, alphabet, annotationFilters, sortColumn, sortDirection, searchInput),
                    (ex) -> logger.error("Error fetching lexicon entries: ", ex));
            if(entries == null){
                response.status(500);
                return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
            }

            model.put("entries", entries);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "/wiki/lexicon/entryList.ftl"));
        } catch (Exception ex) {
            logger.error("Error getting entries from the lexicon - best refer to the last logged API call " +
                    "with id=" + request.attribute("id") + " to this endpoint for URI parameters.", ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

    public Route getLinkableNode = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var requestBody = gson.fromJson(request.body(), Map.class);
        var languageResources = LanguageResources.fromRequest(request);

        var unique = ExceptionUtils.tryCatchLog(() -> requestBody.get("unique"),
                (ex) -> logger.error("Error, getting a linkable node without unique identifier.", ex));
        if(unique == null){
            model.put("information", languageResources.get("missingParameterError"));
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "defaultError.ftl"));
        }

        try {
            // Fetch the Linkable generically, no matter the original model type
            var split = unique.toString().split("-");
            var className = split[0];
            var id = Long.parseLong(split[1]);
            var linkable = db.getLinkable(id, className);

            // Now create the DTO from it.
            var linkableDto = new LinkableNodeDto(linkable);
            linkableDto.setNodeHtml(Renderer.renderLinkable(linkable));
            var linkableVm = linkable.getLinkableViewModel();

            // Here we resolve the incoming and outgoing links into simply: to and from nodes
            linkableDto.fromNodes = new ArrayList<>();
            for(var incoming:linkableVm.getIncomingLinks()){
                var newLinkableDto = new LinkableNodeDto(incoming.getFromLinkableViewModel().getBaseModel());
                newLinkableDto.setNodeHtml(Renderer.renderLinkable(incoming.getFromLinkableViewModel().getBaseModel()));
                newLinkableDto.setLink(incoming.getLink());
                linkableDto.fromNodes.add(newLinkableDto);
            }

            linkableDto.toNodes = new ArrayList<>();
            for(var outgoing:linkableVm.getOutgoingLinks()){
                var newLinkableDto = new LinkableNodeDto(outgoing.getToLinkableViewModel().getBaseModel());
                newLinkableDto.setNodeHtml(Renderer.renderLinkable(outgoing.getToLinkableViewModel().getBaseModel()));
                newLinkableDto.setLink(outgoing.getLink());
                linkableDto.toNodes.add(newLinkableDto);
            }

            return gson.toJson(linkableDto);
        } catch (Exception ex) {
            logger.error("Error getting linkable - best refer to the last logged API call " +
                    "with id=" + request.attribute("id") + " to this endpoint for URI parameters.", ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
    });
}
