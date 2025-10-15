package org.texttechnologylab.uce.web.routes;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import freemarker.template.Configuration;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;
import org.texttechnologylab.uce.common.models.WikiModel;
import org.texttechnologylab.uce.common.models.biofid.GazetteerTaxon;
import org.texttechnologylab.uce.common.models.biofid.GnFinderTaxon;
import org.texttechnologylab.uce.common.models.dto.LinkableNodeDto;
import org.texttechnologylab.uce.common.models.viewModels.wiki.CachedWikiPage;
import org.texttechnologylab.uce.common.services.JenaSparqlService;
import org.texttechnologylab.uce.common.services.LexiconService;
import org.texttechnologylab.uce.common.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.uce.common.services.WikiService;
import org.texttechnologylab.uce.common.utils.SystemStatus;
import org.texttechnologylab.uce.web.CustomFreeMarkerEngine;
import org.texttechnologylab.uce.web.LanguageResources;
import org.texttechnologylab.uce.web.SessionManager;
import org.texttechnologylab.uce.web.freeMarker.Renderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WikiApi implements UceApi {

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

    public void queryOntology(Context ctx) {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        var requestBody = gson.fromJson(ctx.body(), Map.class);

        try {
            var languageResources = LanguageResources.fromRequest(ctx);

            // TODO: Think about adding and handling the truplettype (sub, obj, pred) here.
            var tripletType = ExceptionUtils.tryCatchLog(() -> requestBody.get("tripletType").toString(),
                    (ex) -> logger.error("Need the type of the Triplet to query the sparql database.", ex));
            var value = ExceptionUtils.tryCatchLog(() -> requestBody.get("value").toString(),
                    (ex) -> logger.error("Need the value of the Triplet to execute the query.", ex));

            if (tripletType == null || tripletType.isEmpty() || value == null || value.isEmpty()) {
                model.put("information", languageResources.get("missingParameterError"));
                ctx.render("defaultError.ftl");
                return;
            }

            var nodes = jenaSparqlService.queryBySubject(value);
            // We don't need those children rdf nodes that are the exact same as the queried pne
            model.put("rdfNodes", nodes.stream().filter(n -> !n.getObject().getValue().equals(value)).toList());
            ctx.render("/wiki/components/rdfNodeList.ftl", model);
        } catch (Exception ex) {
            logger.error("Error querying the ontology in the graph database " +
                         "with id=" + ctx.attribute("id") + " to this endpoint for URI parameters.", ex);
            ctx.render("defaultError.ftl");
        }
    }

    public void getPage(Context ctx) {
        var model = new HashMap<String, Object>();

        try {
            var languageResources = LanguageResources.fromRequest(ctx);

            var wid = ExceptionUtils.tryCatchLog(() -> ctx.queryParam("wid"),
                    (ex) -> logger.error("The WikiView couldn't be generated - id missing.", ex));
            var coveredText = ExceptionUtils.tryCatchLog(() -> ctx.queryParam("covered"),
                    (ex) -> logger.error("The WikiView couldn't be generated - covered text missing.", ex));
            // It's actually fine if no additional params were given. I'm not sure anymore I'll use them anyways.
            var params = ExceptionUtils.tryCatchLog(() -> ctx.queryParam("params"), (ex) -> {
            });

            // TODO: logging
            if (wid == null || !wid.contains("-") || coveredText == null || coveredText.isEmpty()) {
                model.put("information", languageResources.get("missingParameterError"));
                ctx.render("defaultError.ftl");
                return;
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
                ctx.result(((CachedWikiPage) SessionManager.CachedWikiPages.get(cacheId)).getRenderedView());
                return;
            }

            // A missing id isn't necessarily bad, as we also have documentation pages etc.
            var id = ExceptionUtils.tryCatchLog(() -> Long.parseLong(split[1]), (ex) -> {
            });

            var renderView = "";
            if (type.startsWith("DOC")) {
                model.put("jenaSparqlAlive", SystemStatus.JenaSparqlStatus.isAlive());
                if (split[1].equals("SEARCH")) {
                    model.put("vm", wikiService.buildDocumentationWikiPageViewModel());
                    renderView = "/wiki/pages/searchDocumentation.ftl";
                }
            } else if (type.startsWith("NE")) {
                // We then clicked onto a Named-Entity wiki item
                model.put("vm", wikiService.buildNamedEntityWikiPageViewModel(id, coveredText));
                renderView = "/wiki/pages/namedEntityAnnotationPage.ftl";
            } else if (type.startsWith("TA")) {
                // We then clicked onto a Taxon wiki item, but which one?
                var clazz = type.equals("TA_GN") ? GnFinderTaxon.class : GazetteerTaxon.class;
                model.put("vm", wikiService.buildTaxonWikipageViewModel(id, coveredText, clazz));
                renderView = "/wiki/pages/taxonAnnotationPage.ftl";
            } else if (type.equals("TP") || type.equals("TD")) {
                // TP = TopicPage TD = TopicDocument
                model.put("vm", wikiService.buildTopicAnnotationWikiPageViewModel(id, type, coveredText));
                renderView = "/wiki/pages/topicAnnotationPage.ftl";
            } else if (type.equals("TI")) {
                // Then we have a Time annotation
                model.put("vm", wikiService.buildTimeAnnotationWikiPageViewModel(id, coveredText));
                renderView = "/wiki/pages/timeAnnotationPage.ftl";
            } else if (type.equals("LOC")) {
                // Then we have a GeoName annotation
                model.put("vm", wikiService.buildGeoNameAnnotationWikiPageViewModel(id, coveredText));
                renderView = "/wiki/pages/geoNameAnnotationPage.ftl";
            } else if (type.equals("SENT")) {
                // Then we have a Sentence annotation
                model.put("vm", wikiService.buildSentenceAnnotationWikiPageViewModel(id));
                renderView = "/wiki/pages/sentenceAnnotationPage.ftl";
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
            } else {
                // The type part of the wikiId was unknown. Throw an error.
                logger.warn("Someone tried to query a wiki page of a type that does not exist in UCE. This shouldn't happen.");
                model.put("information", languageResources.get("missingParameterError"));
                ctx.render("defaultError.ftl");
                return;
            }

            // cache and return the wiki page
            var view = new CustomFreeMarkerEngine(this.freemarkerConfig).render(renderView, model, ctx);
            var cachedWikiPage = new CachedWikiPage(view);
            SessionManager.CachedWikiPages.put(cacheId, cachedWikiPage);
            ctx.result(view);
        } catch (Exception ex) {
            logger.error("Error getting a wiki page - best refer to the last logged API call " +
                         "with id=" + ctx.attribute("id") + " to this endpoint for URI parameters.", ex);
            ctx.render("defaultError.ftl");
        }
    };

    public void getOccurrencesOfLexiconEntry(Context ctx) throws IOException {
        var model = new HashMap<String, Object>();
        var languageResources = LanguageResources.fromRequest(ctx);
        var requestBody = gson.fromJson(ctx.body(), Map.class);

        var coveredText = ExceptionUtils.tryCatchLog(() -> requestBody.get("coveredText").toString(),
                (ex) -> logger.error("Couldn't fetch occurrences of lexicon entry - coveredText missing.", ex));
        var type = ExceptionUtils.tryCatchLog(() -> requestBody.get("type").toString(),
                (ex) -> logger.error("Couldn't fetch occurrences of lexicon entry - type missing.", ex));
        var skip = ExceptionUtils.tryCatchLog(() -> (int) Double.parseDouble(requestBody.get("skip").toString()),
                (ex) -> logger.error("Calling a lexicon entry without skip shouldn't happen.", ex));
        var take = ExceptionUtils.tryCatchLog(() -> (int) Double.parseDouble(requestBody.get("take").toString()),
                (ex) -> logger.error("Calling a lexicon entry without take shouldn't happen.", ex));
        if (coveredText == null || type == null || skip == null || take == null) {
            model.put("information", languageResources.get("missingParameterError"));
            ctx.render("defaultError.ftl");
            return;
        }

        try {
            var occurrences = ExceptionUtils.tryCatchLog(
                    () -> lexiconService.getOccurrenceViewModelsOfEntry(coveredText, type, skip, take),
                    (ex) -> logger.error("Error fetching lexicon entries: ", ex));
            if (occurrences == null) {
                ctx.status(500);
                ctx.render("defaultError.ftl");
                return;
            }

            model.put("occurrences", occurrences);
            ctx.render("/wiki/lexicon/occurrencesList.ftl", model);
        } catch (Exception ex) {
            logger.error("Error getting occurrences from a lexicon entry - best refer to the last logged API call " +
                         "with id=" + ctx.attribute("id") + " to this endpoint for URI parameters.", ex);
            ctx.render("defaultError.ftl");
        }
    }

    public void getLexicon(Context ctx) throws IOException {
        var model = new HashMap<String, Object>();
        var requestBody = gson.fromJson(ctx.body(), Map.class);
        var languageResources = LanguageResources.fromRequest(ctx);

        var skip = ExceptionUtils.tryCatchLog(() -> (int) Double.parseDouble(requestBody.get("skip").toString()),
                (ex) -> logger.error("Calling a lexicon without skip shouldn't happen.", ex));
        var take = ExceptionUtils.tryCatchLog(() -> (int) Double.parseDouble(requestBody.get("take").toString()),
                (ex) -> logger.error("Calling a lexicon without take shouldn't happen.", ex));
        if (skip == null || take == null) {
            model.put("information", languageResources.get("missingParameterError"));
            ctx.render("defaultError.ftl", model);
            return;
        }

        // These parameters are optional. We can work without just fine.
        var alphabet = ExceptionUtils.tryCatchLog(() -> (List<String>) requestBody.get("alphabet"), (ex) -> {
        });
        var annotationFilters = ExceptionUtils.tryCatchLog(() -> (List<String>) requestBody.get("annotationFilters"), (ex) -> {
        });
        var sortColumn = ExceptionUtils.tryCatchLog(() -> requestBody.get("sortColumn").toString(), (ex) -> {
        });
        var sortDirection = ExceptionUtils.tryCatchLog(() -> requestBody.get("sortDirection").toString(), (ex) -> {
        });
        var searchInput = ExceptionUtils.tryCatchLog(() -> requestBody.get("searchInput").toString(), (ex) -> {
        });

        try {
            var entries = ExceptionUtils.tryCatchLog(
                    () -> lexiconService.getEntries(skip, take, alphabet, annotationFilters, sortColumn, sortDirection, searchInput),
                    (ex) -> logger.error("Error fetching lexicon entries: ", ex));
            if (entries == null) {
                ctx.status(500);
                ctx.render("defaultError.ftl");
                return;
            }

            model.put("entries", entries);
            var renderedView = new CustomFreeMarkerEngine(this.freemarkerConfig).render("/wiki/lexicon/entryList.ftl", model, ctx);
            var result = new HashMap<String, Object>();
            result.put("rendered", renderedView);
            result.put("entries", entries);
            ctx.json(result);
        } catch (Exception ex) {
            logger.error("Error getting entries from the lexicon - best refer to the last logged API call " +
                         "with id=" + ctx.attribute("id") + " to this endpoint for URI parameters.", ex);
            ctx.render("defaultError.ftl");
        }
    }

    public void getLinkableNode(Context ctx) throws IOException {
        var model = new HashMap<String, Object>();
        var requestBody = gson.fromJson(ctx.body(), Map.class);
        var languageResources = LanguageResources.fromRequest(ctx);

        var unique = ExceptionUtils.tryCatchLog(() -> requestBody.get("unique"),
                (ex) -> logger.error("Error, getting a linkable node without unique identifier.", ex));
        if (unique == null) {
            model.put("information", languageResources.get("missingParameterError"));
            ctx.render("defaultError.ftl");
            return;
        }

        try {
            // Fetch the Linkable generically, no matter the original model type
            var split = unique.toString().split("-");
            var className = split[0];
            var id = Long.parseLong(split[1]);
            var linkable = db.getLinkable(id, className);

            // Now create the DTO from it.
            var linkableDto = new LinkableNodeDto(linkable);
            linkableDto.setNodeHtml(Renderer.renderLinkable(linkable, null));
            var linkableVm = linkable.getLinkableViewModel();

            // Here we resolve the incoming and outgoing links into simply: to and from nodes
            linkableDto.fromNodes = new ArrayList<>();
            for (var incoming : linkableVm.getIncomingLinks()) {
                if(incoming.getFromLinkableViewModel() == null) continue;
                var newLinkableDto = new LinkableNodeDto(incoming.getFromLinkableViewModel().getBaseModel());
                newLinkableDto.setNodeHtml(Renderer.renderLinkable(incoming.getFromLinkableViewModel().getBaseModel(), linkable));
                newLinkableDto.setLink(incoming.getLink());
                linkableDto.fromNodes.add(newLinkableDto);
            }

            linkableDto.toNodes = new ArrayList<>();
            for (var outgoing : linkableVm.getOutgoingLinks()) {
                if(outgoing.getToLinkableViewModel() == null) continue;
                var newLinkableDto = new LinkableNodeDto(outgoing.getToLinkableViewModel().getBaseModel());
                newLinkableDto.setNodeHtml(Renderer.renderLinkable(outgoing.getToLinkableViewModel().getBaseModel(), linkable));
                newLinkableDto.setLink(outgoing.getLink());
                linkableDto.toNodes.add(newLinkableDto);
            }

            //ctx.result(gson.toJson(linkableDto));
            ctx.json(linkableDto);
        } catch (Exception ex) {
            logger.error("Error getting linkable - best refer to the last logged API call " +
                         "with id=" + ctx.attribute("id") + " to this endpoint for URI parameters.", ex);
            ctx.render("defaultError.ftl");
        }
    }

    public void getAnnotation(Context ctx) {
        var model = new HashMap<String, Object>();

        try {
            var languageResources = LanguageResources.fromRequest(ctx);

            var id = ExceptionUtils.tryCatchLog(() -> Long.parseLong(ctx.queryParam("id")),
                    (ex) -> logger.error("Couldn't fetch annotation - id missing.", ex));
            var annotationClass = ExceptionUtils.tryCatchLog(() -> ctx.queryParam("class"),
                    (ex) -> logger.error("Couldn't fetch annotation - annotation class missing.", ex));

            if (id == null || annotationClass == null || annotationClass.isEmpty()) {
                model.put("information", languageResources.get("missingParameterError"));
                ctx.render("defaultError.ftl");
                return;
            }
            // We need a special gson that handles the lazy loaded proxies of Hibernate.
            var specialGson = new GsonBuilder()
                    .setExclusionStrategies(new ExclusionStrategy() {
                        @Override
                        public boolean shouldSkipField(FieldAttributes f) {
                            // Skip java.lang.Class fields (like HibernateProxy#getClass)
                            return f.getDeclaredType() == Class.class;
                        }

                        @Override
                        public boolean shouldSkipClass(Class<?> clazz) {
                            // Skip hibernate proxies
                            return clazz.getName().contains("org.hibernate.proxy");
                        }
                    })
                    .create();

            var linkableAnnotation = db.getLinkable(id, annotationClass);
            // This right here is why Hibernate has no fking friends. Their lazy loading, initializing logic
            // does whatever it wants to do and hence I have to STORE the page text and ADD IT manually after
            // jsonifiying it. If you find a solution for this - don't tell me, don't care.
            var pageText = ((UIMAAnnotation) linkableAnnotation).getPage().getCoveredText();
            var jsonTree = specialGson.toJsonTree(linkableAnnotation).getAsJsonObject();
            if (linkableAnnotation instanceof WikiModel wikiModel)
                jsonTree.addProperty("wikiId", wikiModel.getWikiId());
            if (jsonTree.has("page") && jsonTree.get("page").isJsonObject()) {
                jsonTree.getAsJsonObject("page").addProperty("coveredText", pageText);
            }
            ctx.result(specialGson.toJson(jsonTree));
        } catch (Exception ex) {
            logger.error("Error getting an annotation - best refer to the last logged API call " +
                         "with id=" + ctx.attribute("id") + " to this endpoint for URI parameters.", ex);
            ctx.render("defaultError.ftl");
        }
    }

}
