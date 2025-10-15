package org.texttechnologylab.uce.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import freemarker.template.Configuration;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.MultipartConfigElement;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.texttechnologylab.uce.analysis.modules.CohMetrixInfo;
import org.texttechnologylab.uce.analysis.modules.ModelGroup;
import org.texttechnologylab.uce.analysis.modules.ModelResources;
import org.texttechnologylab.uce.analysis.modules.TTLabScorerInfo;
import org.texttechnologylab.uce.common.config.CommonConfig;
import org.texttechnologylab.uce.common.config.SpringConfig;
import org.texttechnologylab.uce.common.config.UceConfig;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.models.corpus.Corpus;
import org.texttechnologylab.uce.common.models.corpus.UCELog;
import org.texttechnologylab.uce.common.services.LexiconService;
import org.texttechnologylab.uce.common.services.MapService;
import org.texttechnologylab.uce.common.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.uce.common.utils.ImageUtils;
import org.texttechnologylab.uce.common.utils.StringUtils;
import org.texttechnologylab.uce.common.utils.SystemStatus;
import org.texttechnologylab.uce.search.LayeredSearch;
import org.texttechnologylab.uce.web.auth.AuthenticationRouteRegister;
import org.texttechnologylab.uce.web.freeMarker.Renderer;
import org.texttechnologylab.uce.web.freeMarker.RequestContextHolder;
import org.texttechnologylab.uce.web.routes.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static io.javalin.apibuilder.ApiBuilder.*;

public class App {
    private static final Configuration configuration = Configuration.getDefaultConfiguration();
    private static final Logger logger = LogManager.getLogger(App.class);
    private static CommonConfig commonConfig = null;
    private static boolean forceLexicalization = false;
    private static int DUUIInputCounter = 0;

    public static void main(String[] args) throws IOException {
        logger.info("Starting the UCE web service...");
        logger.info("Passed in command line args: " + String.join(" ", args));

        logger.info("Parsing the UCE config...");
        try {
            parseCommandLine(args);
            logger.info("UCE Config read and instantiated.");
        } catch (MissingOptionException ex) {
            logger.error("UCE couldn't parse the UceConfig in the CLI properly. " +
                    "UCE will still start with a default config, but this is not a desirable state.", ex);
        } catch (Exception ex) {
            logger.error("Couldn't parse the CLI arguments properly - app shutting down.", ex);
            return;
        }

        commonConfig = new CommonConfig();
        logger.info("Loaded the common config.");

        logger.info("Adjusting UCE to the UceConfig...");
        ExceptionUtils.tryCatchLog(
                () -> implementUceConfigurations(commonConfig),
                (ex) -> logger.error("Couldn't implement the UceConfig. Application continues running.", ex));

        // Application context for services
        var context = ExceptionUtils.tryCatchLog(
                () -> new AnnotationConfigApplicationContext(SpringConfig.class),
                (ex) -> logger.fatal("========== [ABORT] ==========\nThe Application context couldn't be established. " +
                        "This is very likely due to a missing/invalid database connection. UCE will have to shutdown."));
        if(context == null) return;
        logger.info("Loaded application context and services.");

        // Execute the external database scripts
        logger.info("Executing external database scripts from " + commonConfig.getDatabaseScriptsLocation());
        ExceptionUtils.tryCatchLog(
                () -> SystemStatus.executeExternalDatabaseScripts(commonConfig.getDatabaseScriptsLocation(), context.getBean(PostgresqlDataInterface_Impl.class)),
                (ex) -> logger.warn("Couldn't read the db scripts in the external database scripts folder; path wasn't found or other IO problems. ", ex));
        logger.info("Finished with executing external database scripts.");

        // Cleanup temporary db fragments for the LayeredSearch
        ExceptionUtils.tryCatchLog(
                () -> LayeredSearch.CleanupScheme(context.getBean(PostgresqlDataInterface_Impl.class)),
                (ex) -> logger.warn("Error while trying to cleanup the LayeredSearch temporary schema.", ex));
        logger.info("Cleanup temporary LayeredSearch tables.");

        // Load in and test the language translation objects to handle multiple languages
        logger.info("Testing the language resources:");
        var languageResource = new LanguageResources("en-EN");
        logger.info(languageResource.get("search"));

        // Load in and test the model resources for the Analysis Engine
        if(SystemStatus.UceConfig.getSettings().getAnalysis().isEnableAnalysisEngine()){
            var modelResources = new ModelResources();
            var ttlabScorer = new TTLabScorerInfo();
            var cohMetrixInfo = new CohMetrixInfo();
            logger.info("Testing the model resources:");
        }

        // Start the different cronjobs in the background
        SessionManager.InitSessionManager(commonConfig.getSessionJobInterval());
        logger.info("Initialized the Session Job.");

        SystemStatus.initSystemStatus(commonConfig.getSystemJobInterval(), context);
        logger.info("Initialized the System Job.");

        logger.info("Checking if we can or should update the lexicon... (this may take a moment depending on the time of the last update. Runs asynchronous.)");
        CompletableFuture.runAsync(() -> {
            SystemStatus.LexiconIsCalculating = true;
            var lexiconService = context.getBean(LexiconService.class);
            var addedLexiconEntries = 0;
            if(forceLexicalization) addedLexiconEntries = lexiconService.updateLexicon(true);
            else addedLexiconEntries = lexiconService.checkForUpdates();
            logger.info("Finished updating the lexicon. Added new entries: " + addedLexiconEntries);
            SystemStatus.LexiconIsCalculating = false;
        });

        logger.info("Checking if we can or should update any linkables... (this may take a moment depending on the time of the last update. Runs asynchronous.)");
        CompletableFuture.runAsync(() -> {
            try{
                var result = context.getBean(PostgresqlDataInterface_Impl.class).callLogicalLinksRefresh();
                logger.info("Finished updating the linkables. Updated linkables: " + result);
            } catch (Exception ex){
                logger.error("There was an error trying to refresh linkables in the startup of the web app. App starts normally though.");
            }
        });

        logger.info("Checking if we can or should update any geoname locations... (this may take a moment depending on the time of the last update. Runs asynchronous.)");
        CompletableFuture.runAsync(() -> {
            try{
                var result = context.getBean(PostgresqlDataInterface_Impl.class).callGeonameLocationRefresh();
                logger.info("Finished updating the geoname locations. Updated locations: " + result);
                logger.info("Trying to refresh the timeline map cache...");
                context.getBean(MapService.class).refreshCachedTimelineMap(false);
                logger.info("Finished refreshing the timeline map.");
            } catch (Exception ex){
                logger.error("There was an error trying to refresh geoname locations in the startup of the web app. App starts normally though.");
            }
        });

        // Set the folder for our template files of freemarker
        configuration.setDirectoryForTemplateLoading(new File(commonConfig.getTemplatesLocation()));
        configuration.setDefaultEncoding("UTF-8");

        var registry = new ApiRegistry(context, configuration, DUUIInputCounter);

        var mapper = getJsonMapper();
        logger.info("Setting up the Javalin application...");
        var javalinApp = Javalin.create(config -> {
            try {
                // We use the externalLocation method so that the files in the public folder are hot reloaded
                if (commonConfig.useExternalPublicLocation()) {
                    config.staticFiles.add(commonConfig.getPublicLocation(), Location.EXTERNAL);
                }
                else {
                    config.staticFiles.add("/public", Location.CLASSPATH);
                }
                logger.info("Setup FreeMarker templates and public folders.");
            } catch (Exception e) {
                logger.error("Error setting up FreeMarker, the application will hence shutdown.", e);
                return;
            }
            config.fileRenderer(new CustomFreeMarkerEngine(configuration));
            logger.info("Setup FreeMarker templates and public folders.");

            // Start the routes.
            logger.info("Initializing all the spark routes...");
            ExceptionUtils.tryCatchLog(() -> initSparkRoutes(context, registry, config),
                    (ex) -> logger.error("There was a problem initializing the spark routes, web service will be shut down.", ex));
            logger.info("Routes initialized");

            // Start MCP server
            if (SystemStatus.UceConfig.getSettings().getMcp().isEnabled()) {
                logger.info("Initializing MCP server...");
                ExceptionUtils.tryCatchLog(() -> initMCP(registry, config),
                        (ex) -> logger.error("There was a problem initializing the MCP server, web service will be shut down.", ex));
                logger.info("MCP server initialized.");
            }
            else {
                logger.info("MCP server is disabled and will not be initialized.");
            }
            config.jsonMapper(mapper);
        });

        // Define default exception handler. This shows an error view then in the body.
        // TODO why cant this be done in "config"?
        javalinApp.exception(
                Exception.class,
                (exception, ctx) -> {
                    logger.error("Unknown error handled in API - returning default error view.", exception);
                    ctx.status(500);
                    ctx.render("defaultError.ftl");
                }
        );

        if (SystemStatus.UceConfig.getSettings().getAuthentication().isActivated()) {
            AuthenticationRouteRegister.registerApis(registry.getAll(), javalinApp);
        }

        logger.info("Javalin application setup done.");

        int port = 4567;
        if (SystemStatus.UceConfig.getSettings().getPort() != null) {
            port = SystemStatus.UceConfig.getSettings().getPort();
        }
        javalinApp.start(port);

        logger.info("UCE web service has started!");
    }

    /**
     * Implements the different parameters of the UceConfig such as thematic appearance and such.
     */
    private static void implementUceConfigurations(CommonConfig commonConfig) throws Exception {
        // First, set the corpora identities
        // Colors
        var siteCss = new File(commonConfig.getTemplatesLocation() + "css/site.css");
        var lines = Files.readAllLines(siteCss.toPath());
        lines.set(2, "    --prime: " + SystemStatus.UceConfig.getCorporate().getPrimaryColor() + ";");
        lines.set(3, "    --secondary: " + SystemStatus.UceConfig.getCorporate().getSecondaryColor() + ";");
        Files.write(siteCss.toPath(), lines, StandardOpenOption.TRUNCATE_EXISTING);

        // Logo
        SystemStatus.UceConfig.getCorporate().setLogo(convertConfigImageString(SystemStatus.UceConfig.getCorporate().getLogo()));

        // Team Members
        for (var member : SystemStatus.UceConfig.getCorporate().getTeam().getMembers())
            member.setImage(convertConfigImageString(member.getImage()));
    }

    /**
     * Converts a img string in the config to a proper usable base64encoded image
     *
     * @param imgString
     * @return
     * @throws IOException
     */
    private static String convertConfigImageString(String imgString) throws Exception {
        if (imgString.startsWith("BASE64::")) {
            // If the logo is a base64 string, we only need to remove the prefix
            return imgString.replace("BASE64::", "");
        } else if (imgString.startsWith("FILE::")) {
            // else we need to read in the file from the given path.
            var path = imgString.replace("FILE::", "");
            return ImageUtils.EncodeImageToBase64(path);
        }
        return "";
    }

    /**
     * Parsing the CLI to various configs.
     */
    private static void parseCommandLine(String[] args) throws ParseException, FileNotFoundException {
        var options = new Options();
        options.addOption("cf", "configFile", true, "The filepath to the UceConfig.json file.");
        options.addOption("cj", "configJson", true, "The json content of a UceConfig.json file.");
        options.addOption("lex", "forceLexicalization", false, "Force the full lexicalization of all annotations. " +
                "This process may take a while but will be executed asynchronous.");

        var parser = new DefaultParser();
        var gson = new Gson();

        var cmd = parser.parse(options, args);
        forceLexicalization = cmd.hasOption("forceLexicalization");
        var configFile = cmd.getOptionValue("configFile");
        var configJson = cmd.getOptionValue("configJson");

        if (configFile != null && !configFile.isEmpty()) {
            var reader = new FileReader(configFile);
            SystemStatus.UceConfig = gson.fromJson(reader, UceConfig.class);
            logger.info("Read UCE Config from path: " + configFile);
        } else if (configJson != null && !configJson.isEmpty()) {
            SystemStatus.UceConfig = gson.fromJson(configJson, UceConfig.class);
            logger.info("Parsed UCE Config from JSON.");
        }

        // If we haven't gotten a proper config, then we will use a default
        if (SystemStatus.UceConfig == null) {
            var inputStream = App.class.getClassLoader().getResourceAsStream("defaultUceConfig.json");
            if (inputStream != null) {
                SystemStatus.UceConfig = gson.fromJson(new InputStreamReader(inputStream), UceConfig.class);
            } else {
                throw new RuntimeException("Default uceConfig.json not found in the classpath.");
            }

            // We still throw an exception here, stating that we had to default to the default config.
            // UCE may run like this, but it's NOT a wanted state.
            throw new MissingOptionException("UCE couldn't establish its UceConfiguration properly. " +
                    "Either pass in a path to the config json file via -cf or the json content directly via -cj.");
        }
    }

    private static void initMCP(ApiRegistry registry, JavalinConfig config) throws Exception {
        config.jetty.modifyServletContextHandler(context -> {
            HttpServletStreamableServerTransportProvider transportProvider = HttpServletStreamableServerTransportProvider
                    .builder()
                    .objectMapper(new ObjectMapper())
                    .mcpEndpoint("/mcp")
                    .build();
            context.addServlet(new ServletHolder(transportProvider), "/mcp/*");

            McpSyncServer mcpServer = McpServer.sync(transportProvider)
                    .serverInfo("ttlab-uce", "0.0.1")
                    .capabilities(McpSchema.ServerCapabilities.builder()
                            .resources(false, false)
                            .tools(true)
                            .prompts(true)
                            .logging()
                            .completions()
                            .build())
                    .build();

            (registry.get(McpApi.class)).registerTools(mcpServer);
        });
    }

    private static void initSparkRoutes(ApplicationContext context, ApiRegistry registry, JavalinConfig config) throws IOException {
        Renderer.freemarkerConfig = configuration;

        ModelResources modelResources = new ModelResources();
        TTLabScorerInfo ttlabScorer = new TTLabScorerInfo();
        CohMetrixInfo cohMetrixInfo = new CohMetrixInfo();
        LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>> taInputMap = ttlabScorer.getTaInputMap();
        LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>> cohMetrixMap = cohMetrixInfo.getCohMetrixMap();
        List<ModelGroup> groups = modelResources.getGroupedModelObjects();

        config.router.apiBuilder(() -> {
                    before(ctx -> {
                        ctx.res().setCharacterEncoding("UTF-8");
                        // Setup and log all API calls with some information. We don't want to log file uploads, since it would
                        // destroy the file body stream.
                        if (!(ctx.contentType() != null && ctx.contentType().contains("multipart/form-data"))) {
                            ctx.attribute("id", UUID.randomUUID().toString());
                            logger.info("Received API call: ID={}, IP={}, Method={}, URI={}, QUERY={}, BODY={}",
                                    ctx.attribute("id"), ctx.ip(), ctx.method().name(), ctx.url(), ctx.queryString(), ctx.body());

                            // Should we log to db as well?
                            if (commonConfig.getLogToDb() && SystemStatus.PostgresqlDbStatus.isAlive()) {
                                var uceLog = new UCELog(ctx.ip(), ctx.method().name(), ctx.url(), ctx.body(), ctx.queryString());
                                ExceptionUtils.tryCatchLog(
                                        () -> context.getBean(PostgresqlDataInterface_Impl.class).saveUceLog(uceLog),
                                        (ex) -> logger.error("Error storing a log to the database: ", ex));
                                logger.info("Last log was also logged to the db with id " + uceLog.getId());
                            }
                        } else {
                            // Else we have a form-data upload. We handle those explicitly.
                            // Set the multipart data configs for uploads
                            ctx.req().setAttribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/tmp"));
                        }

                        // Always inject the current system config into all UI templates
                        RequestContextHolder.setUceConfigHolder(SystemStatus.UceConfig);

                        // Check if the request contains a language parameter
                        var languageResources = LanguageResources.fromRequest(ctx);
                        ctx.header("Content-Language", languageResources.getDefaultLanguage());
                        RequestContextHolder.setLanguageResources(languageResources);

                        // Check if we have an authenticated user in the session and inject it into the template
                        if (SystemStatus.UceConfig.getSettings().getAuthentication().isActivated()) {
                            var user = SessionManager.getUserFromRequest(ctx);
                            RequestContextHolder.setAuthenticatedUceUser(user);
                        }
                    });

                    // Landing page
                    get("/", ctx -> {
                        var model = new HashMap<String, Object>();
                        model.put("title", SystemStatus.UceConfig.getMeta().getName());
                        model.put("corpora", context.getBean(PostgresqlDataInterface_Impl.class)
                                .getAllCorpora()
                                .stream().map(Corpus::getViewModel)
                                .toList());
                        model.put("commonConf", commonConfig);
                        model.put("isSparqlAlive", SystemStatus.JenaSparqlStatus.isAlive());
                        model.put("isAuthAlive", SystemStatus.AuthenticationService.isAlive());
                        model.put("isDbAlive", SystemStatus.PostgresqlDbStatus.isAlive());
                        model.put("isRagAlive", SystemStatus.RagServiceStatus.isAlive());
                        model.put("isS3StorageAlive", SystemStatus.S3StorageStatus.isAlive());
                        model.put("isLexiconCalculating", SystemStatus.LexiconIsCalculating);
                        model.put("alphabetList", StringUtils.getAlphabetAsList());
                        model.put("lexiconEntriesCount", context.getBean(LexiconService.class).countLexiconEntries());
                        model.put("lexiconizableAnnotations", LexiconService.lexiconizableAnnotations);
                        model.put("uceVersion", commonConfig.getUceVersion());
                        model.put("modelGroups", groups);
                        model.put("ttlabScorer", taInputMap);
                        model.put("cohMetrix", cohMetrixMap);

                        // The vm files are located under the resources directory
                        ctx.render("index.ftl", model);
                    });

                    // Potential imprint
                    get("/imprint", ctx -> {
                        var model = new HashMap<String, Object>();
                        model.put("imprint", SystemStatus.UceConfig.getCorporate().getImprint());
                        ctx.render("imprint.ftl", model);
                    });

                    // A document reader view
                    get("/documentReader", (ctx) -> (registry.get(DocumentApi.class)).getSingleDocumentReadView(ctx));

                    // A corpus World View
                    get("/globe", (ctx) -> (registry.get(DocumentApi.class)).get3dGlobe(ctx));


                    path("/auth", () -> {
                        get("/login", (ctx) -> (registry.get(AuthenticationApi.class)).loginCallback(ctx));
                        get("/logout", (ctx) -> (registry.get(AuthenticationApi.class)).logoutCallback(ctx));
                    });

                    // API routes
                    path("/api", () -> {
                        before("/*", (ctx) -> {
                        });

                        path("/ie", () -> {
                            post("/upload/uima", (ctx) -> (registry.get(ImportExportApi.class)).uploadUIMA(ctx));
                            get("/download/uima", (ctx) -> (registry.get(ImportExportApi.class)).downloadUIMA(ctx));
                        });

                        path("/wiki", () -> {
                            get("/page", (ctx) -> (registry.get(WikiApi.class)).getPage(ctx));
                            get("/annotation", (ctx) -> (registry.get(WikiApi.class)).getAnnotation(ctx));
                            path("/linkable", () -> {
                                post("/node", (ctx) -> (registry.get(WikiApi.class)).getLinkableNode(ctx));
                            });
                            path("/lexicon", () -> {
                                post("/entries", (ctx) -> (registry.get(WikiApi.class)).getLexicon(ctx));
                                post("/occurrences", (ctx) -> (registry.get(WikiApi.class)).getOccurrencesOfLexiconEntry(ctx));
                            });
                            post("/queryOntology", (ctx) -> (registry.get(WikiApi.class)).queryOntology(ctx));
                        });

                        path("/corpus", () -> {
                            get("/inspector", (ctx) -> (registry.get(DocumentApi.class)).getCorpusInspectorView(ctx));
                            get("/documentsList", (ctx) -> (registry.get(DocumentApi.class)).getDocumentListOfCorpus(ctx));
                            path("/map", () -> {
                                post("/linkedOccurrences", (ctx) -> (registry.get(MapApi.class)).getLinkedOccurrences(ctx));
                                post("/linkedOccurrenceClusters", (ctx) -> (registry.get(MapApi.class)).getLinkedOccurrenceClusters(ctx));
                            });
                        });

                        path("/search", () -> {
                            post("/default", (ctx) -> (registry.get(SearchApi.class)).search(ctx));
                            post("/semanticRole", (ctx) -> (registry.get(SearchApi.class)).semanticRoleSearch(ctx));
                            post("/layered", (ctx) -> (registry.get(SearchApi.class)).layeredSearch(ctx));
                            get("/active/page", (ctx) -> (registry.get(SearchApi.class)).activeSearchPage(ctx));
                            get("/active/sort", (ctx) -> (registry.get(SearchApi.class)).activeSearchSort(ctx));
                            get("/semanticRole/builder", (ctx) -> (registry.get(SearchApi.class)).getSemanticRoleBuilderView(ctx));
                        });

                        path("/analysis", () -> {
                            post("/runPipeline", (ctx) -> (registry.get(AnalysisApi.class)).runPipeline(ctx));
                            get("/setHistory", (ctx) -> (registry.get(AnalysisApi.class)).setHistory(ctx));
                            post("/callHistory", (ctx) -> (registry.get(AnalysisApi.class)).callHistory(ctx));
                            post("/callHistoryText", (ctx) -> (registry.get(AnalysisApi.class)).callHistoryText(ctx));
                        });

                        path("/corpusUniverse", () -> {
                            // Gets a corpus universe view
                            get("/new", (ctx) -> (registry.get(CorpusUniverseApi.class)).getCorpusUniverseView(ctx));
                            post("/fromSearch", (ctx) -> (registry.get(CorpusUniverseApi.class)).fromSearch(ctx));
                            post("/fromCorpus", (ctx) -> (registry.get(CorpusUniverseApi.class)).fromCorpus(ctx));
                            get("/nodeInspectorContent", (ctx) -> (registry.get(CorpusUniverseApi.class)).getNodeInspectorContentView(ctx));
                        });

                        path("/document", () -> {
                            get("/reader/pagesList", (ctx) -> (registry.get(DocumentApi.class)).getPagesListView(ctx));
                            get("/uceMetadata", (ctx) -> (registry.get(DocumentApi.class)).getUceMetadataOfDocument(ctx));
                            get("/topics", (ctx) -> (registry.get(DocumentApi.class)).getDocumentTopics(ctx));
                            get("/page/taxon", (ctx) -> (registry.get(DocumentApi.class)).getTaxonCountByPage(ctx));
                            get("/page/topics", (ctx) -> (registry.get(DocumentApi.class)).getDocumentTopicDistributionByPage(ctx));
                            get("/page/topicEntityRelation", (ctx) -> (registry.get(DocumentApi.class)).getSentenceTopicsWithEntities(ctx));
                            get("/page/topicWords", (ctx) -> (registry.get(DocumentApi.class)).getTopicWordsByDocument(ctx));
                            get("/unifiedTopicSentenceMap", (ctx) -> (registry.get(DocumentApi.class)).getUnifiedTopicToSentenceMap(ctx));
                            get("/page/namedEntities", (ctx) -> (registry.get(DocumentApi.class)).getDocumentNamedEntitiesByPage(ctx));
                            get("/page/lemma", (ctx) -> (registry.get(DocumentApi.class)).getDocumentLemmaByPage(ctx));
                            get("/page/geoname", (ctx) -> (registry.get(DocumentApi.class)).getDocumentGeonameByPage(ctx));
                            delete("/delete", (ctx) -> (registry.get(DocumentApi.class)).deleteDocument(ctx));
                            get("/findIdByMetadata", (ctx) -> (registry.get(DocumentApi.class)).findDocumentIdByMetadata(ctx));
                            get("/findIdsByMetadata", (ctx) -> (registry.get(DocumentApi.class)).findDocumentIdsByMetadata(ctx));
                        });

                        path("/rag", () -> {
                            get("/new", (ctx) -> (registry.get(RAGApi.class)).getNewRAGChat(ctx));
                            // NOTE we allow also "post" here, as the system prompt can get quite long...
                            post("/new", (ctx) -> (registry.get(RAGApi.class)).getNewRAGChat(ctx));
                            post("/postUserMessage", (ctx) -> (registry.get(RAGApi.class)).postUserMessage(ctx));
                            get("/messages", (ctx) -> (registry.get(RAGApi.class)).getMessagesForChat(ctx));
                            get("/plotTsne", (ctx) -> (registry.get(RAGApi.class)).getTsnePlot(ctx));
                            get("/sentenceEmbeddings", (ctx) -> (registry.get(RAGApi.class)).getSentenceEmbeddings(ctx));
                        });
                    });
                });
    }

    private static JsonMapper getJsonMapper() {
        Gson gson = new GsonBuilder().create();
        return new JsonMapper() {
            @NotNull
            @Override
            public String toJsonString(@NotNull Object obj, @NotNull Type type) {
                if (type == String.class) {
                    return (String) obj;
                }
                return gson.toJson(obj, type);
            }

            @NotNull
            @Override
            public <T> T fromJsonString(@NotNull String json, @NotNull Type targetType) {
                return gson.fromJson(json, targetType);
            }
        };
    }
}
