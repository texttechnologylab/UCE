package org.texttechnologylab.routes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import freemarker.template.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.CustomFreeMarkerEngine;
import org.texttechnologylab.LanguageResources;
import org.texttechnologylab.SearchState;
import org.texttechnologylab.SessionManager;
import org.texttechnologylab.config.CorpusConfig;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.corpus.UCEMetadataValueType;
import org.texttechnologylab.models.search.SearchType;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import spark.ModelAndView;
import spark.Route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentApi {
    private PostgresqlDataInterface_Impl db;
    private static final Logger logger = LogManager.getLogger(DocumentApi.class);
    private Configuration freemarkerConfig;

    public DocumentApi(ApplicationContext serviceContext, Configuration freemarkerConfig) {
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.freemarkerConfig = freemarkerConfig;
    }

    public Route getUceMetadataOfDocument = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var languageResources = LanguageResources.fromRequest(request);

        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId and hence can't return the metadata. ", ex));
        if (documentId == null) {
            model.put("information", languageResources.get("missingParameterError"));
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }

        try {
            var uceMetadata = db.getUCEMetadataByDocumentId(documentId);
            var gson = new GsonBuilder().setPrettyPrinting().create();
            uceMetadata.forEach(m -> {
                if (m.getValueType() == UCEMetadataValueType.JSON) {
                    var obj = gson.fromJson(m.getValue(), Object.class);
                    m.setValue(gson.toJson(obj));
                }
            });
            model.put("uceMetadata", uceMetadata);
        } catch (Exception ex) {
            logger.error("Error getting the uce metadata of a document.", ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }

        return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "document/documentUceMetadata.ftl"));
    });

    public Route getDocumentListOfCorpus = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var languageResources = LanguageResources.fromRequest(request);

        var corpusId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("corpusId")),
                (ex) -> logger.error("Error: couldn't determine the corpusId and hence can't return the document list. ", ex));
        if (corpusId == null) {
            model.put("information", languageResources.get("missingParameterError"));
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
        var page = ExceptionUtils.tryCatchLog(() -> Integer.parseInt(request.queryParams("page")),
                (ex) -> logger.error("Error: couldn't determine the page, defaulting to page 1 then. ", ex));
        if (page == null) page = 1;

        try {
            var take = 10;
            var documents = db.getDocumentsByCorpusId(corpusId, (page - 1) * take, take);

            model.put("requestId", request.attribute("id"));
            model.put("documents", documents);
            model.put("corpusId", corpusId);
        } catch (Exception ex) {
            logger.error("Error getting the documents list of a corpus.", ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }

        // Depending on the page, we returns JUST a rendered list of documents or
        // a view that contains the documents but also styles navigation and such
        if (page == 1)
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "corpus/components/corpusDocumentsList.ftl"));
        else
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "corpus/components/documents.ftl"));
    });

    public Route getCorpusInspectorView = ((request, response) -> {
        var model = new HashMap<String, Object>();

        var corpusId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("id")),
                (ex) -> logger.error("Error: the url for the corpus inspector requires an 'id' query parameter that is the corpusId. ", ex));
        if (corpusId == null)
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));

        try {
            var corpus = db.getCorpusById(corpusId);
            var corpusConfig = CorpusConfig.fromJson(corpus.getCorpusJsonConfig());
            var documentsCount = db.countDocumentsInCorpus(corpusId);

            model.put("corpus", corpus);
            model.put("corpusConfig", corpusConfig);
            model.put("documentsCount", documentsCount);

        } catch (Exception ex) {
            logger.error("Error getting the corpus inspector view.", ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }

        return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "corpus/corpusInspector.ftl"));
    });

    public Route get3dGlobe = ((request, response) -> {
        var model = new HashMap<String, Object>();

        var id = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("id")),
                (ex) -> logger.error("Error: the url for the document 3d globe requires an 'id' query parameter that is the document id.", ex));
        if (id == null)
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));

        try {
            // I've forgotten why I introduced this variable here?...
            //var type = request.queryParams("type");
            var document = db.getDocumentById(id);
            var data = db.getGlobeDataForDocument(id);
            var gson = new Gson();
            var dataJson = gson.toJson(data);

            model.put("document", document);
            model.put("data", data);
            model.put("jsonData", dataJson);
        } catch (Exception ex) {
            logger.error("Error getting the 3D globe of a document, returning default error view.", ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }

        return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "corpus/globe.ftl"));
    });

    public Route getSingleDocumentReadView = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var gson = new Gson();

        var id = ExceptionUtils.tryCatchLog(() -> request.queryParams("id"),
                (ex) -> logger.error("Error: the url for the document reader requires an 'id' query parameter. " +
                                     "Document reader can't be built.", ex));
        if (id == null)
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));

        // Check if we have an searchId parameter. This is optional
        var searchId = ExceptionUtils.tryCatchLog(() -> request.queryParams("searchId"),
                (ex) -> logger.warn("Opening a document view but no searchId parameter was provided. Currently, this shouldn't happen, but it didn't stop the procedure."));

        try {
            var doc = db.getCompleteDocumentById(Long.parseLong(id), 0, 10);
            model.put("document", doc);

            // If this document was opened from an active search, we can highlight the search tokens in the text
            // This is only optional and works fine even without the search tokens.
            if (searchId != null && SessionManager.ActiveSearches.containsKey(searchId)) {
                var activeSearchState = (SearchState) SessionManager.ActiveSearches.get(searchId);
                // For SRL Search, there are no search tokens really. We will handle that exclusively later.
                if (activeSearchState.getSearchType() != SearchType.SEMANTICROLE || activeSearchState.getSearchType() != SearchType.NEG) {
                    if (activeSearchState.getSearchTokens() != null)
                        model.put("searchTokens", String.join("[TOKEN]", activeSearchState.getSearchTokens()));
                }
            }
        } catch (Exception ex) {
            logger.error("Error creating the document reader view for document with id: " + id, ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }

        return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "reader/documentReaderView.ftl"));
    });

    public Route getPagesListView = ((request, response) -> {

        var model = new HashMap<String, Object>();

        var id = ExceptionUtils.tryCatchLog(() -> request.queryParams("id"),
                (ex) -> logger.error("Error: the url for the document pages list view requires an 'id' query parameter. ", ex));
        if (id == null)
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));

        try {
            var skip = Integer.parseInt(request.queryParams("skip"));
            var doc = db.getCompleteDocumentById(Long.parseLong(id), skip, 10);
            var annotations = doc.getAllAnnotations(skip, 10);
            model.put("documentAnnotations", annotations);
            model.put("documentText", doc.getFullText());
            model.put("documentPages", doc.getPages(10, skip));
        } catch (Exception ex) {
            logger.error("Error getting the pages list view - either the document couldn't be fetched (id=" + id + ") or its annotations.", ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }

        return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "reader/components/pagesList.ftl"));
    });

    public Route getDocumentTopics = ((request, response) -> {
        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for topics. ", ex));

        if (documentId == null) {
            response.status(400);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(Map.of("information", "Missing documentId parameter"), "defaultError.ftl"));
        }

        try {
            var limit = Integer.parseInt(request.queryParams("limit"));

            var topTopics = db.getTopTopicsByDocument(documentId, limit);
            var result = new ArrayList<Map<String, Object>>();

            for (Object[] topic : topTopics) {
                var topicMap = new HashMap<String, Object>();
                topicMap.put("label", topic[0]);
                topicMap.put("probability", topic[1]);
                result.add(topicMap);
            }

            response.type("application/json");
            return new Gson().toJson(result);
        } catch (Exception ex) {
            logger.error("Error getting document topics.", ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(Map.of("information", "Error retrieving document topics."), "defaultError.ftl"));
        }
    });

    public Route getTaxonCountByPage = ((request, response) -> {
        var documentId = request.queryParams("documentId");

        if (documentId == null || documentId.isEmpty()) {
            response.status(400);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Missing documentId parameter"), "defaultError.ftl"));
        }

        try {
            var taxonValuesAndCounts = db.getTaxonValuesAndCountByPageId(Long.parseLong(documentId));
            var result = new ArrayList<Map<String, Object>>();

            for (Object[] row : taxonValuesAndCounts) {
                var pageMap = new HashMap<String, Object>();
                pageMap.put("pageId", row[0]);
                pageMap.put("taxonValue", row[1]);
                //var taxonValues = row[1].toString().replaceAll("[\\{\\}]", "").split(",");
                //pageMap.put("taxon_values", taxonValues);
                //pageMap.put("taxon_count", row[2]);
                result.add(pageMap);
            }

            response.type("application/json");
            return new Gson().toJson(result);
        } catch (Exception ex) {
            logger.error("Error getting taxon counts.", ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Error retrieving taxon counts."), "defaultError.ftl"));
        }
    });

    public Route getDocumentTopicDistributionByPage = ((request, response) -> {
        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for topics. ", ex));

        if (documentId == null) {
            response.status(400);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Missing documentId parameter"), "defaultError.ftl"));
        }

        try {
            var topicDistPerPage = db.getTopicDistributionByPageForDocument(documentId);
            var result = new ArrayList<Map<String, Object>>();

            for (Object[] row : topicDistPerPage) {
                var pageMap = new HashMap<String, Object>();
                pageMap.put("pageId", row[0]);
                pageMap.put("topicLabel", row[1]);
                result.add(pageMap);
            }

            response.type("application/json");
            return new Gson().toJson(result);
        } catch (Exception ex) {
            logger.error("Error getting document topics.", ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Error retrieving document topics."), "defaultError.ftl"));
        }
    });

    public Route getDocumentNamedEntitiesByPage = ((request, response) -> {
        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for entities. ", ex));

        if (documentId == null) {
            response.status(400);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Missing documentId parameter"), "defaultError.ftl"));
        }

        try {
            var entitiesPerPage = db.getNamedEntityValuesAndCountByPage(documentId);
            var result = new ArrayList<Map<String, Object>>();

            for (Object[] row : entitiesPerPage) {
                var pageMap = new HashMap<String, Object>();
                pageMap.put("pageId", row[0]);
                pageMap.put("entityValue", row[1]);
                pageMap.put("entityType", row[2]);
                result.add(pageMap);
            }

            response.type("application/json");
            return new Gson().toJson(result);
        } catch (Exception ex) {
            logger.error("Error getting document entities.", ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Error retrieving document entities."), "defaultError.ftl"));
        }
    });

    public Route getDocumentLemmaByPage = ((request, response) -> {
        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for lemma. ", ex));

        if (documentId == null) {
            response.status(400);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Missing documentId parameter"), "defaultError.ftl"));
        }

        try {
            var lemmaPerPage = db.getLemmaByPage(documentId);
            var result = new ArrayList<Map<String, Object>>();

            for (Object[] row : lemmaPerPage) {
                var pageMap = new HashMap<String, Object>();
                pageMap.put("pageId", row[0]);
                pageMap.put("lemmaValue", row[1]);
                pageMap.put("coarseValue", row[2]);
                result.add(pageMap);
            }

            response.type("application/json");
            return new Gson().toJson(result);
        } catch (Exception ex) {
            logger.error("Error getting document lemma.", ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Error retrieving document lemma."), "defaultError.ftl"));
        }
    });

    public Route getDocumentGeonameByPage = ((request, response) -> {
        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for geoname. ", ex));

        if (documentId == null) {
            response.status(400);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Missing documentId parameter"), "defaultError.ftl"));
        }

        try {
            var geonamePerPage = db.getGeonameByPage(documentId);
            var result = new ArrayList<Map<String, Object>>();

            for (Object[] row : geonamePerPage) {
                var pageMap = new HashMap<String, Object>();
                pageMap.put("pageId", row[0]);
                pageMap.put("geonameValue", row[1]);
                result.add(pageMap);
            }

            response.type("application/json");
            return new Gson().toJson(result);
        } catch (Exception ex) {
            logger.error("Error getting document geoname.", ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Error retrieving document geoname."), "defaultError.ftl"));
        }
    });


    public Route getSentenceTopicsWithEntities = ((request, response) -> {
        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for sentence topics with entities. ", ex));

        if (documentId == null) {
            response.status(400);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Missing documentId parameter for sentence topics with entities"), "defaultError.ftl"));
        }

        try {
            var topicsWithEntities = db.getSentenceTopicsWithEntitiesByPageForDocument(documentId);
            var result = new ArrayList<Map<String, Object>>();

            for (Object[] row : topicsWithEntities) {
                var topicEntityMap = new HashMap<String, Object>();
                topicEntityMap.put("topicLabel", row[0]);
                topicEntityMap.put("entityType", row[1]);
                result.add(topicEntityMap);
            }

            response.type("application/json");
            return new Gson().toJson(result);
        } catch (Exception ex) {
            logger.error("Error getting sentence topics with entities.", ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Error retrieving sentence topics with entities."), "defaultError.ftl"));
        }
    });

    public Route getTopicWordsByDocument = (request, response) -> {
        Long documentId = ExceptionUtils.tryCatchLog(
                () -> Long.parseLong(request.queryParams("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for topic words.", ex)
        );

        if (documentId == null) {
            response.status(400);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Missing documentId parameter for topic words"), "defaultError.ftl"));
        }

        try {
            var topicWords = db.getTopicWordsByDocumentId(documentId);
            var groupedResults = new HashMap<String, Map<String, Double>>();

            for (Object[] row : topicWords) {
                String topicLabel = (String) row[0];
                String word = (String) row[1];
                Double avgProbability = ((Number) row[2]).doubleValue();

                groupedResults.computeIfAbsent(topicLabel, k -> new HashMap<>())
                        .put(word, avgProbability);
            }

            var result = new ArrayList<Map<String, Object>>();
            for (var entry : groupedResults.entrySet()) {
                var topicMap = new HashMap<String, Object>();
                topicMap.put("topicLabel", entry.getKey());
                topicMap.put("words", entry.getValue());
                result.add(topicMap);
            }

            response.type("application/json");
            return new Gson().toJson(result);

        } catch (Exception ex) {
            logger.error("Error getting topic words by document ID.", ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Error retrieving topic words."), "defaultError.ftl"));
        }
    };

    public Route getUnifiedTopicToSentenceMap = (request, response) -> {
        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(request.queryParams("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for unified topic to sentence mapping.", ex));

        if (documentId == null) {
            response.status(400);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Missing documentId parameter for unified topic to sentence mapping"), "defaultError.ftl"));
        }

        try {
            Map<Long, Long> mapping = db.getUnifiedTopicToSentenceMap(documentId);
            List<Map<String, Object>> result = new ArrayList<>();

            for (var entry : mapping.entrySet()) {
                Map<String, Object> mapEntry = new HashMap<>();
                mapEntry.put("unifiedtopicId", entry.getKey());
                mapEntry.put("sentenceId", entry.getValue());
                result.add(mapEntry);
            }

            response.type("application/json");
            return new Gson().toJson(result);

        } catch (Exception ex) {
            logger.error("Error retrieving unified topic to sentence mapping.", ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(Map.of("information", "Error retrieving unified topic to sentence mapping."), "defaultError.ftl"));
        }
    };


}
