package org.texttechnologylab.uce.web.routes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import freemarker.template.Configuration;
import io.javalin.http.Context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.uce.common.config.CorpusConfig;
import org.texttechnologylab.uce.common.exceptions.DatabaseOperationException;
import org.texttechnologylab.uce.common.exceptions.DocumentAccessDeniedException;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.models.authentication.UceUser;
import org.texttechnologylab.uce.common.models.corpus.UCEMetadataValueType;
import org.texttechnologylab.uce.common.models.search.SearchType;
import org.texttechnologylab.uce.common.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.uce.common.services.S3StorageService;
import org.texttechnologylab.uce.search.SearchState;
import org.texttechnologylab.uce.web.LanguageResources;
import org.texttechnologylab.uce.web.SessionManager;
import org.texttechnologylab.uce.web.freeMarker.AccessDeniedRenderer;
import org.texttechnologylab.uce.web.render.DefaultPaneRenderer;
import org.texttechnologylab.uce.web.render.RenderContext;
import org.texttechnologylab.uce.web.render.RenderException;
import org.texttechnologylab.uce.web.render.RenderPrincipal;
import org.texttechnologylab.uce.web.render.RenderModeDescriptor;
import org.texttechnologylab.uce.web.render.RenderResult;
import org.texttechnologylab.uce.web.render.RendererRegistry;
import org.texttechnologylab.uce.web.render.feedback.FeedbackDocument;
import org.texttechnologylab.uce.web.render.feedback.FeedbackDocumentMapper;
import org.texttechnologylab.uce.web.render.feedback.FeedbackPaneRenderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.texttechnologylab.models.authentication.DocumentPermission;
import org.texttechnologylab.uce.common.config.corpusConfig.RenderModeConfig;
import org.texttechnologylab.uce.common.security.DocumentAccessContext;
import org.texttechnologylab.uce.common.security.DocumentAccessManager;

public class DocumentApi implements UceApi {
    private S3StorageService s3StorageService;
    private PostgresqlDataInterface_Impl db;
    private static final Logger logger = LogManager.getLogger(DocumentApi.class);
    private Configuration freemarkerConfig;

    private final FeedbackDocumentMapper feedbackMapper = new FeedbackDocumentMapper();
    private final RendererRegistry rendererRegistry;

    public DocumentApi(ApplicationContext serviceContext, Configuration freemarkerConfig) {
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.s3StorageService = serviceContext.getBean(S3StorageService.class);
        this.freemarkerConfig = freemarkerConfig;

        this.rendererRegistry = serviceContext.getBean(RendererRegistry.class);
    }

    public void getUceMetadataOfDocument(Context ctx) throws IOException {
        var model = new HashMap<String, Object>();
        var languageResources = LanguageResources.fromRequest(ctx);

        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(ctx.queryParam("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId and hence can't return the metadata. ", ex));
        if (documentId == null) {
            model.put("information", languageResources.get("missingParameterError"));
            ctx.render("defaultError.ftl");
            return;
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
        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } catch (Exception ex) {
            logger.error("Error getting the uce metadata of a document.", ex);
            ctx.render("defaultError.ftl");
            return;
        }

        ctx.render("document/documentUceMetadata.ftl", model);
    }

    public void getDocumentListOfCorpus(Context ctx) throws IOException {
        var model = new HashMap<String, Object>();
        var languageResources = LanguageResources.fromRequest(ctx);

        var corpusId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(ctx.queryParam("corpusId")),
                (ex) -> logger.error("Error: couldn't determine the corpusId and hence can't return the document list. ", ex));
        if (corpusId == null) {
            model.put("information", languageResources.get("missingParameterError"));
            ctx.render("defaultError.ftl", model);
            return;
        }
        var page = ExceptionUtils.tryCatchLog(() -> Integer.parseInt(ctx.queryParam("page")),
                (ex) -> logger.error("Error: couldn't determine the page, defaulting to page 1 then. ", ex));
        if (page == null) page = 1;

        try {
            var take = 10;
            var documents = db.getDocumentsByCorpusId(corpusId, (page - 1) * take, take);

            model.put("requestId", ctx.attribute("id"));
            model.put("documents", documents);
            model.put("corpusId", corpusId);
        
        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } catch (Exception ex) {
            logger.error("Error getting the documents list of a corpus.", ex);
            ctx.render("defaultError.ftl");
            return;
        }

        // Depending on the page, we returns JUST a rendered list of documents or
        // a view that contains the documents but also styles navigation and such
        if (page == 1)
            ctx.render("corpus/components/corpusDocumentsList.ftl", model);
        else
            ctx.render("corpus/components/documents.ftl", model);
    }

    public void getCorpusInspectorView(Context ctx) {

        var model = new HashMap<String, Object>();

        var corpusId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(ctx.queryParam("id")),
                (ex) -> logger.error("Error: the url for the corpus inspector requires an 'id' query parameter that is the corpusId. ", ex));
        if (corpusId == null) {
            ctx.render("defaultError.ftl");
            return;
        }

        try {
            var corpus = db.getCorpusById(corpusId);
            var corpusConfig = CorpusConfig.fromJson(corpus.getCorpusJsonConfig());
            var documentsCount = db.countDocumentsInCorpus(corpusId);
            var pagesCount = db.countPagesInCorpus(corpusId);

            model.put("corpus", corpus);
            model.put("corpusConfig", corpusConfig);
            model.put("documentsCount", documentsCount);
            model.put("pagesCount", pagesCount);

        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } catch (Exception ex) {
            logger.error("Error getting the corpus inspector view.", ex);
            ctx.render("defaultError.ftl");
            return;
        }

        ctx.render("corpus/corpusInspector.ftl", model);
    }

    public void get3dGlobe(Context ctx) {

        var model = new HashMap<String, Object>();

        var id = ExceptionUtils.tryCatchLog(() -> Long.parseLong(ctx.queryParam("id")),
                (ex) -> logger.error("Error: the url for the document 3d globe requires an 'id' query parameter that is the document id.", ex));
        if (id == null) {
            ctx.render("defaultError.ftl");
            return;
        }

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
        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } catch (Exception ex) {
            logger.error("Error getting the 3D globe of a document, returning default error view.", ex);
            ctx.render("defaultError.ftl");
            return;
        }

        ctx.render("corpus/globe.ftl", model);
    }

    public void getSingleDocumentReadView(Context ctx) {

        var model = new HashMap<String, Object>();

        var id = ExceptionUtils.tryCatchLog(() -> ctx.queryParam("id"),
                (ex) -> logger.error("Error: the url for the document reader requires an 'id' query parameter. " +
                                     "Document reader can't be built.", ex));
        if (id == null) {
            ctx.render("defaultError.ftl");
            return;
        }

        // Check if we have an searchId parameter. This is optional
        var searchId = ExceptionUtils.tryCatchLog(() -> ctx.queryParam("searchId"),
                (ex) -> logger.warn("Opening a document view but no searchId parameter was provided. Currently, this shouldn't happen, but it didn't stop the procedure."));
        if (searchId != null) {
            searchId = searchId.trim();
            if (searchId.isEmpty() || "undefined".equalsIgnoreCase(searchId) || "null".equalsIgnoreCase(searchId)) {
                searchId = null;
            }
        }

        try {

            var doc = db.getCompleteDocumentById(Long.parseLong(id), 0, 10);
            model.put("document", doc);

            var corpus = db.getCorpusById(doc.getCorpusId());
            var corpusConfig = CorpusConfig.fromJson(corpus.getCorpusJsonConfig());
            var casDownloadName = s3StorageService.buildCasXmiObjectName(corpus.getId(), doc.getDocumentId());
            var casDownloadExists = s3StorageService.objectExists(casDownloadName);
            model.put("casDownloadName", casDownloadExists ? casDownloadName : "");

            // Build render mode descriptors
            var modes = buildRenderModes(corpusConfig);
            if (modes.isEmpty()) {
                modes = List.of(buildDefaultPdfMode());
            }

            var requestedMode = ctx.queryParam("mode");
            var selectedKey = (requestedMode != null && modes.stream().anyMatch(m -> m.key().equals(requestedMode)))
                    ? requestedMode
                    : modes.get(0).key();

            var activeMode = modes.stream()
                    .filter(m -> m.key().equals(selectedKey))
                    .findFirst()
                    .orElse(modes.get(0));

            model.put("renderModes", modes);
            model.put("activeMode", activeMode.key());
            model.put("activeModeKey", activeMode.key());
            model.put("activeModeHandler", activeMode.handler());

            var renderer = rendererRegistry
                    .renderer(activeMode.handler())
                    .orElseGet(() -> rendererRegistry.renderer(DefaultPaneRenderer.HANDLER_KEY).orElseThrow());

            UceUser currentUser = ctx.sessionAttribute("uceUser");
            var principal = currentUser != null ? currentUser.getUsername() : DocumentPermission.PUBLIC_USERNAME;

            var activeModeConfig = findRenderModeConfig(corpusConfig, activeMode.key()).orElse(null);

            var renderContextBuilder = RenderContext.builder(corpus, doc)
                    .payload(RenderPrincipal.class, new RenderPrincipal(principal));
            if (activeModeConfig != null) {
                renderContextBuilder.payload(RenderModeConfig.class, activeModeConfig);
            }
            if (FeedbackPaneRenderer.HANDLER_KEY.equals(activeMode.handler())) {
                var feedback = feedbackMapper.map(doc, principal);
                renderContextBuilder.payload(FeedbackDocument.class, feedback);
            }
            var renderContext = renderContextBuilder.build();

            RenderResult panes;
            try {
                panes = renderer.render(renderContext);
            } catch (RenderException ex) {
                panes = rendererRegistry.renderer(DefaultPaneRenderer.HANDLER_KEY).orElseThrow()
                        .render(renderContext);
                // Fallback to PDF view as a last resort if the configured renderer fails.
                if (modes.stream().noneMatch(m -> m.key().equals(DefaultPaneRenderer.HANDLER_KEY))) {
                    modes = new ArrayList<>(modes);
                    modes.add(buildDefaultPdfMode());
                    model.put("renderModes", modes);
                }
                model.put("activeMode", DefaultPaneRenderer.HANDLER_KEY);
                model.put("activeModeKey", DefaultPaneRenderer.HANDLER_KEY);
                model.put("activeModeHandler", DefaultPaneRenderer.HANDLER_KEY);
            }

            model.put("middlePaneTemplate", panes.getMiddlePaneTemplate());
            model.put("middlePaneModel", panes.getMiddlePaneModel());
            model.put("hasRightPane", panes.hasRightPane());
            model.put("rightPaneTemplate", panes.getRightPaneTemplate());
            model.put("rightPaneModel", panes.getRightPaneModel());
            model.put("document", doc);
            model.put("corpus", corpus);

            // If this document was opened from an active search, we can highlight the search tokens in the text
            // This is only optional and works fine even without the search tokens.
            if (searchId != null && SessionManager.ActiveSearches.containsKey(searchId)) {
                var activeSearchState = (SearchState) SessionManager.ActiveSearches.get(searchId);
                // For SRL Search, there are no search tokens really. We will handle that exclusively later.
                if (activeSearchState.getSearchType() != SearchType.SEMANTICROLE && activeSearchState.getSearchType() != SearchType.NEG) {
                    if (activeSearchState.getSearchTokens() != null)
                        model.put("searchTokens", String.join("[TOKEN]", activeSearchState.getSearchTokens()));
                }
            }
        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } catch (Exception ex) {
            logger.error("Error creating the document reader view for document with id: " + id, ex);
            ctx.render("defaultError.ftl");
            return;
        }

        ctx.render("reader/documentReaderView.ftl", model);
    };

    private List<RenderModeDescriptor> buildRenderModes(CorpusConfig config) {
        List<RenderModeDescriptor> descriptors = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        if (config != null && config.getRenderModes() != null) {
            for (RenderModeConfig mode : config.getRenderModes()) {
                if (mode == null || mode.getKey() == null) {
                    continue;
                }
                if (seenKeys.contains(mode.getKey())) { // Skip duplicates
                    continue;
                }

                // Only include render modes that have an actual renderer registered.
                if (mode.getHandler() == null || rendererRegistry.renderer(mode.getHandler()).isEmpty()) {
                    continue;
                }

                descriptors.add(new RenderModeDescriptor(
                        mode.getKey(),
                        mode.getName(),
                        mode.getHandler(),
                        mode.getDescription()
                ));
                seenKeys.add(mode.getKey());
            }
        }

        // Only include PDF view if explicitly configured, or as a last resort if nothing else is available.
        boolean pdfExplicitlyConfigured = config != null
                && config.getRenderModes() != null
                && config.getRenderModes().stream().anyMatch(m -> m != null && DefaultPaneRenderer.HANDLER_KEY.equals(m.getKey()));
        boolean pdfAlreadyPresent = descriptors.stream().anyMatch(m -> DefaultPaneRenderer.HANDLER_KEY.equals(m.key()));
        if (descriptors.isEmpty() || (pdfExplicitlyConfigured && !pdfAlreadyPresent)) {
            descriptors.add(buildDefaultPdfMode());
        }
        return descriptors;
    }

    private RenderModeDescriptor buildDefaultPdfMode() {
        return new RenderModeDescriptor(
                DefaultPaneRenderer.HANDLER_KEY,
                "PDF",
                DefaultPaneRenderer.HANDLER_KEY,
                null
        );
    }

    private Optional<RenderModeConfig> findRenderModeConfig(CorpusConfig config, String key) {
        if (config == null || config.getRenderModes() == null || key == null) {
            return Optional.empty();
        }
        return config.getRenderModes()
                .stream()
                .filter(mode -> mode != null && key.equals(mode.getKey()))
                .findFirst();
    }

    /**
    * Finds all document ids matching a metadata key, value and value type.
    */
    public void findDocumentIdsByMetadata(Context ctx) {

        var key = ExceptionUtils.tryCatchLog(() -> ctx.queryParam("key"),
                (ex) -> logger.error("Error: document deletion requires a 'key' query parameter. ", ex));
        var value = ExceptionUtils.tryCatchLog(() -> ctx.queryParam("value"),
                (ex) -> logger.error("Error: document deletion requires a 'value' query parameter. ", ex));
        var valueTypeStr = ExceptionUtils.tryCatchLog(() -> ctx.queryParam("value_type"),
                (ex) -> logger.error("Error: document deletion requires a 'value_type' query parameter (e.g. STRING, NUMBER, ...). ", ex));
        if (key == null || value == null || valueTypeStr == null) {
            ctx.render("defaultError.ftl");
            return;
        }

        try {
            UCEMetadataValueType valueType = UCEMetadataValueType.valueOf(valueTypeStr);

            List<Long> documentIds = db.findDocumentIdsByMetadata(key, value, valueType);

            Map<String, Object> result = new HashMap<>();
            result.put("document_ids", documentIds);
            ctx.json(result);
        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } catch (Exception ex) {
            logger.error(ex);
            ctx.status(500);
            ctx.render("defaultError.ftl");
        }
    }

    /**
     * Finds the first document id matching a metadata key, value and value type.
    */
    public void findDocumentIdByMetadata(Context ctx){

        var key = ExceptionUtils.tryCatchLog(() -> ctx.queryParam("key"),
                (ex) -> logger.error("Error: document deletion requires a 'key' query parameter. ", ex));
        var value = ExceptionUtils.tryCatchLog(() -> ctx.queryParam("value"),
                (ex) -> logger.error("Error: document deletion requires a 'value' query parameter. ", ex));
        var valueTypeStr = ExceptionUtils.tryCatchLog(() -> ctx.queryParam("value_type"),
                (ex) -> logger.error("Error: document deletion requires a 'value_type' query parameter (e.g. STRING, NUMBER, ...). ", ex));
        if (key == null || value == null || valueTypeStr == null) {
            ctx.render("defaultError.ftl");
            return;
        }

        try {
            UCEMetadataValueType valueType = UCEMetadataValueType.valueOf(valueTypeStr);

            List<Long> documentIds = db.findDocumentIdsByMetadata(key, value, valueType);
            Long documentId = documentIds.getFirst();

            Map<String, Object> result = new HashMap<>();
            result.put("document_id", documentId);
            ctx.json(result);
        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } catch (Exception ex) {
            logger.error(ex);
            ctx.status(500);
            ctx.render("defaultError.ftl");
        }
    }

    public void deleteDocument(Context ctx) throws DatabaseOperationException, NumberFormatException {
        
        var id = ExceptionUtils.tryCatchLog(() -> ctx.queryParam("id"),
                (ex) -> logger.error("Error: document deletion requires an 'id' query parameter. ", ex));
        if (id == null) {
            ctx.render("defaultError.ftl");
            return;
        }

        try {
            db.deleteDocumentById(Long.parseLong(id));
        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } 

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "NOTE Document deletion is not fully implemented yet.");

        ctx.json(result);
    }

    public void getPagesListView(Context ctx) {

        var model = new HashMap<String, Object>();

        var id = ExceptionUtils.tryCatchLog(() -> ctx.queryParam("id"),
                (ex) -> logger.error("Error: the url for the document pages list view requires an 'id' query parameter. ", ex));
        if (id == null) {
            ctx.render("defaultError.ftl");
            return;
        }

        try {
            var skip = Integer.parseInt(ctx.queryParam("skip"));
            var doc = db.getCompleteDocumentById(Long.parseLong(id), skip, 10);
            var annotations = doc.getAllAnnotations(skip, 10);
            model.put("documentAnnotations", annotations);
            model.put("documentText", doc.getFullText());
            model.put("documentPages", doc.getPages(10, skip));
        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } catch (Exception ex) {
            logger.error("Error getting the pages list view - either the document couldn't be fetched (id=" + id + ") or its annotations.", ex);
            ctx.render("defaultError.ftl");
            return;
        }

        ctx.render("reader/components/pagesList.ftl", model);
    }

    public void getDocumentTopics(Context ctx) {

        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(ctx.queryParam("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for topics. ", ex));

        if (documentId == null) {
            ctx.status(400);
            ctx.render("defaultError.ftl", Map.of("information", "Missing documentId parameter"));
            return;
        }

        try {
            var limit = Integer.parseInt(ctx.queryParam("limit"));

            var topTopics = db.getTopTopicsByDocument(documentId, limit);
            var result = new ArrayList<Map<String, Object>>();

            for (Object[] topic : topTopics) {
                var topicMap = new HashMap<String, Object>();
                topicMap.put("label", topic[0]);
                topicMap.put("probability", topic[1]);
                result.add(topicMap);
            }

            ctx.json(result);
        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } catch (Exception ex) {
            logger.error("Error getting document topics.", ex);
            ctx.status(500);
            ctx.render("defaultError.ftl", Map.of("information", "Error retrieving document topics."));
        }
    }

    public void getTaxonCountByPage(Context ctx) {

        var documentId = ctx.queryParam("documentId");

        if (documentId == null || documentId.isEmpty()) {
            ctx.status(400);
            ctx.render("defaultError.ftl", Map.of("information", "Missing documentId parameter"));
            return;
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

            ctx.json(result);
        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } catch (Exception ex) {
            logger.error("Error getting taxon counts.", ex);
            ctx.status(500);
            ctx.render("defaultError.ftl", Map.of("information", "Error retrieving taxon counts."));
        }
    }

    public void getDocumentTopicDistributionByPage(Context ctx) {
        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(ctx.queryParam("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for topics. ", ex));

        if (documentId == null) {
            ctx.status(400);
            ctx.render("defaultError.ftl", Map.of("information", "Missing documentId parameter"));
            return;
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

            ctx.json(result);
        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } catch (Exception ex) {
            logger.error("Error getting document topics.", ex);
            ctx.status(500);
            ctx.render("defaultError.ftl", Map.of("information", "Error retrieving document topics."));
        }
    }

    public void getDocumentNamedEntitiesByPage(Context ctx) {
        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(ctx.queryParam("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for entities. ", ex));

        if (documentId == null) {
            ctx.status(400);
            ctx.render("defaultError.ftl", Map.of("information", "Missing documentId parameter"));
            return;
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

            ctx.json(result);
        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } catch (Exception ex) {
            logger.error("Error getting document entities.", ex);
            ctx.status(500);
            ctx.render("defaultError.ftl", Map.of("information", "Error retrieving document entities."));
        }
    }

    public void getDocumentLemmaByPage(Context ctx) {
        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(ctx.queryParam("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for lemma. ", ex));

        if (documentId == null) {
            ctx.status(400);
            ctx.render("defaultError.ftl", Map.of("information", "Missing documentId parameter"));
            return;
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

            ctx.json(result);
        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } catch (Exception ex) {
            logger.error("Error getting document lemma.", ex);
            ctx.status(500);
            ctx.render("defaultError.ftl", Map.of("information", "Error retrieving document lemma."));
        }
    }

    public void getDocumentGeonameByPage(Context ctx) {
        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(ctx.queryParam("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for geoname. ", ex));

        if (documentId == null) {
            ctx.status(400);
            ctx.render("defaultError.ftl", Map.of("information", "Missing documentId parameter"));
            return;
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

            ctx.json(result);
        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } catch (Exception ex) {
            logger.error("Error getting document geoname.", ex);
            ctx.status(500);
            ctx.render("defaultError.ftl", Map.of("information", "Error retrieving document geoname."));
        }
    }


    public void getSentenceTopicsWithEntities(Context ctx) {
        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(ctx.queryParam("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for sentence topics with entities. ", ex));

        if (documentId == null) {
            ctx.status(400);
            ctx.render("defaultError.ftl", Map.of("information", "Missing documentId parameter for sentence topics with entities"));
            return;
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

            ctx.json(result);
        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } catch (Exception ex) {
            logger.error("Error getting sentence topics with entities.", ex);
            ctx.status(500);
            ctx.render("defaultError.ftl", Map.of("information", "Error retrieving sentence topics with entities."));
        }
    }

    public void getTopicWordsByDocument(Context ctx) {
        Long documentId = ExceptionUtils.tryCatchLog(
                () -> Long.parseLong(ctx.queryParam("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for topic words.", ex)
        );

        if (documentId == null) {
            ctx.status(400);
            ctx.render("defaultError.ftl", Map.of("information", "Missing documentId parameter for topic words"));
            return;
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

            ctx.json(result);

        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } catch (Exception ex) {
            logger.error("Error getting topic words by document ID.", ex);
            ctx.status(500);
            ctx.render("defaultError.ftl", Map.of("information", "Error retrieving topic words."));
        }
    }

    public void getUnifiedTopicToSentenceMap(Context ctx) {
        var documentId = ExceptionUtils.tryCatchLog(() -> Long.parseLong(ctx.queryParam("documentId")),
                (ex) -> logger.error("Error: couldn't determine the documentId for unified topic to sentence mapping.", ex));

        if (documentId == null) {
            ctx.status(400);
            ctx.render("defaultError.ftl", Map.of("information", "Missing documentId parameter for unified topic to sentence mapping"));
            return;
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

            ctx.json(result);

        } catch (DocumentAccessDeniedException dade) {
            AccessDeniedRenderer.render(
                    ctx,
                    dade,
                    logger);
            return;
        } catch (Exception ex) {
            logger.error("Error retrieving unified topic to sentence mapping.", ex);
            ctx.status(500);
            ctx.render("defaultError.ftl", Map.of("information", "Error retrieving unified topic to sentence mapping."));
        }
    }

}
