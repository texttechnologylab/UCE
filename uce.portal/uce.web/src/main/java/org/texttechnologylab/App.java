package org.texttechnologylab;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.texttechnologylab.auth.AuthenticationRouteRegister;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.config.SpringConfig;
import org.texttechnologylab.config.UceConfig;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.freeMarker.Renderer;
import org.texttechnologylab.freeMarker.RequestContextHolder;
import org.texttechnologylab.models.authentication.UceUser;
import org.texttechnologylab.models.corpus.Corpus;
import org.texttechnologylab.models.corpus.UCELog;
import org.texttechnologylab.modules.ModelGroup;
import org.texttechnologylab.modules.ModelResources;
import org.texttechnologylab.modules.TTLabScorerInfo;
import org.texttechnologylab.routes.*;
import org.texttechnologylab.services.LexiconService;
import org.texttechnologylab.services.MapService;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.typeClasses.TtlabScorer;
import org.texttechnologylab.utils.ImageUtils;
import org.texttechnologylab.utils.StringUtils;
import org.texttechnologylab.utils.SystemStatus;
import spark.ExceptionHandler;
import spark.ModelAndView;

import javax.servlet.MultipartConfigElement;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static spark.Spark.*;

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
                () ->LayeredSearch.CleanupScheme(context.getBean(PostgresqlDataInterface_Impl.class)),
                (ex) -> logger.warn("Error while trying to cleanup the LayeredSearch temporary schema.", ex));
        logger.info("Cleanup temporary LayeredSearch tables.");

        // Load in and test the language translation objects to handle multiple languages
        logger.info("Testing the language resources:");
        var languageResource = new LanguageResources("en-EN");
        logger.info(languageResource.get("search"));

        // Load in and test the model resources for the Analysis Engine
        if(SystemStatus.UceConfig.getSettings().getAnalysis().isEnableAnalysisEngine()){
            var modelResources = new ModelResources();
            var ttlabScorer = new TtlabScorer();
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
        try {
            configuration.setDirectoryForTemplateLoading(new File(commonConfig.getTemplatesLocation()));

            // We use the externalLocation method so that the files in the public folder are hot reloaded
            staticFiles.externalLocation(commonConfig.getPublicLocation());
            logger.info("Setup FreeMarker templates and public folders.");
        } catch (Exception e) {
            logger.error("Error setting up FreeMarker, the application will hence shutdown.", e);
            return;
        }

        // Start the routes.
        logger.info("Initializing all the spark routes...");
        ExceptionUtils.tryCatchLog(() -> initSparkRoutes(context),
                (ex) -> logger.error("There was a problem initializing the spark routes, web service will be shut down.", ex));
        logger.info("Routes initialized - UCE web service has started!");
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

    private static void initSparkRoutes(ApplicationContext context) throws IOException {
        var registry = new ApiRegistry(context, configuration, DUUIInputCounter);
        if (SystemStatus.UceConfig.getSettings().getAuthentication().isActivated()) {
            AuthenticationRouteRegister.registerApis(registry.getAll());
        }
        Renderer.freemarkerConfig = configuration;

        before((request, response) -> {
            // Setup and log all API calls with some information. We don't want to log file uploads, since it would
            // destroy the file body stream.
            if (!(request.contentType() != null && request.contentType().contains("multipart/form-data"))) {
                request.attribute("id", UUID.randomUUID().toString());
                logger.info("Received API call: ID={}, IP={}, Method={}, URI={}, QUERY={}, BODY={}",
                        request.attribute("id"), request.ip(), request.requestMethod(), request.uri(), request.queryString(), request.body());

                // Should we log to db as well?
                if (commonConfig.getLogToDb() && SystemStatus.PostgresqlDbStatus.isAlive()) {
                    var uceLog = new UCELog(request.ip(), request.requestMethod(), request.uri(), request.body(), request.queryString());
                    ExceptionUtils.tryCatchLog(
                            () -> context.getBean(PostgresqlDataInterface_Impl.class).saveUceLog(uceLog),
                            (ex) -> logger.error("Error storing a log to the database: ", ex));
                    logger.info("Last log was also logged to the db with id " + uceLog.getId());
                }
            } else{
                // Else we have a form-data upload. We handle those explicitly.
                // Set the multipart data configs for uploads
                request.raw().setAttribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/tmp"));
            }

            // Always inject the current system config into all UI templates
            RequestContextHolder.setUceConfigHolder(SystemStatus.UceConfig);

            // Check if the request contains a language parameter
            var languageResources = LanguageResources.fromRequest(request);
            response.header("Content-Language", languageResources.getDefaultLanguage());
            RequestContextHolder.setLanguageResources(languageResources);

            // Check if we have an authenticated user in the session and inject it into the template
            if(SystemStatus.UceConfig.getSettings().getAuthentication().isActivated()){
                var user = SessionManager.getUserFromRequest(request);
                RequestContextHolder.setAuthenticatedUceUser(user);
            }
        });

        ModelResources modelResources = new ModelResources();
        TTLabScorerInfo ttlabScorer = new TTLabScorerInfo();
        LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>> taInputMap = ttlabScorer.getTaInputMap();
        List<ModelGroup> groups = modelResources.getGroupedModelObjects();
        // Landing page
        get("/", (request, response) -> {
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

            // The vm files are located under the resources directory
            return new ModelAndView(model, "index.ftl");
        }, new CustomFreeMarkerEngine(configuration));

        // Potential imprint
        get("/imprint", (request, response) -> {
            var model = new HashMap<String, Object>();
            model.put("imprint", SystemStatus.UceConfig.getCorporate().getImprint());
            return new ModelAndView(model, "imprint.ftl");
        }, new CustomFreeMarkerEngine(configuration));

        // A document reader view
        get("/documentReader", (registry.get(DocumentApi.class)).getSingleDocumentReadView);

        // A corpus World View
        get("/globe", (registry.get(DocumentApi.class)).get3dGlobe);

        // Define default exception handler. This shows an error view then in the body.
        ExceptionHandler<Exception> defaultExceptionHandler = (exception, request, response) -> {
            logger.error("Unknown error handled in API - returning default error view.", exception);
            response.status(500);
            response.body(new CustomFreeMarkerEngine(configuration).render(new ModelAndView(null, "defaultError.ftl")));
        };

        path("/auth", () -> {
            get("/login", (registry.get(AuthenticationApi.class)).loginCallback);
            get("/logout", (registry.get(AuthenticationApi.class)).logoutCallback);
        });

        // API routes
        path("/api", () -> {

            exception(Exception.class, defaultExceptionHandler);

            before("/*", (req, res) -> {
            });

            path("/ie", () -> {
                post("/upload/uima", (registry.get(ImportExportApi.class)).uploadUIMA);
                get("/download/uima", (registry.get(ImportExportApi.class)).downloadUIMA);
            });

            path("/wiki", () -> {
                get("/page", (registry.get(WikiApi.class)).getPage);
                get("/annotation", (registry.get(WikiApi.class)).getAnnotation);
                path("/linkable", () -> {
                    post("/node", (registry.get(WikiApi.class)).getLinkableNode);
                });
                path("/lexicon", () -> {
                    post("/entries", (registry.get(WikiApi.class)).getLexicon);
                    post("/occurrences", (registry.get(WikiApi.class)).getOccurrencesOfLexiconEntry);
                });
                post("/queryOntology", (registry.get(WikiApi.class)).queryOntology);
            });

            path("/corpus", () -> {
                get("/inspector", (registry.get(DocumentApi.class)).getCorpusInspectorView);
                get("/documentsList", (registry.get(DocumentApi.class)).getDocumentListOfCorpus);
                path("/map", () -> {
                    post("/linkedOccurrences", (registry.get(MapApi.class)).getLinkedOccurrences);
                    post("/linkedOccurrenceClusters", (registry.get(MapApi.class)).getLinkedOccurrenceClusters);
                });
            });

            path("/search", () -> {
                post("/default", (registry.get(SearchApi.class)).search);
                post("/semanticRole", (registry.get(SearchApi.class)).semanticRoleSearch);
                post("/layered", (registry.get(SearchApi.class)).layeredSearch);
                get("/active/page", (registry.get(SearchApi.class)).activeSearchPage);
                get("/active/sort", (registry.get(SearchApi.class)).activeSearchSort);
                get("/semanticRole/builder", (registry.get(SearchApi.class)).getSemanticRoleBuilderView);
            });

            path("/analysis", () -> {
                post("/runPipeline", (registry.get(AnalysisApi.class)).runPipeline);
                get("/setHistory", (registry.get(AnalysisApi.class)).setHistory);
                post("/callHistory", (registry.get(AnalysisApi.class)).callHistory);
                post("/callHistoryText", (registry.get(AnalysisApi.class)).callHistoryText);
            });

            path("/corpusUniverse", () -> {
                // Gets a corpus universe view
                get("/new", (registry.get(CorpusUniverseApi.class)).getCorpusUniverseView);
                post("/fromSearch", (registry.get(CorpusUniverseApi.class)).fromSearch);
                post("/fromCorpus", (registry.get(CorpusUniverseApi.class)).fromCorpus);
                get("/nodeInspectorContent", (registry.get(CorpusUniverseApi.class)).getNodeInspectorContentView);
            });

            path("/document", () -> {
                get("/reader/pagesList", (registry.get(DocumentApi.class)).getPagesListView);
                get("/uceMetadata", (registry.get(DocumentApi.class)).getUceMetadataOfDocument);
                get("/topics", (registry.get(DocumentApi.class)).getDocumentTopics);
                get("/page/taxon", (registry.get(DocumentApi.class)).getTaxonCountByPage);
                get("/page/topics", (registry.get(DocumentApi.class)).getDocumentTopicDistributionByPage);
                get("/page/topicEntityRelation", (registry.get(DocumentApi.class)).getSentenceTopicsWithEntities);
                get("/page/topicWords", (registry.get(DocumentApi.class)).getTopicWordsByDocument);
                get("/unifiedTopicSentenceMap", (registry.get(DocumentApi.class)).getUnifiedTopicToSentenceMap);
                get("/page/namedEntities", (registry.get(DocumentApi.class)).getDocumentNamedEntitiesByPage);
                get("/page/lemma", (registry.get(DocumentApi.class)).getDocumentLemmaByPage);
                get("/page/geoname", (registry.get(DocumentApi.class)).getDocumentGeonameByPage);
            });

            path("/rag", () -> {
                get("/new", (registry.get(RAGApi.class)).getNewRAGChat);
                post("/postUserMessage", (registry.get(RAGApi.class)).postUserMessage);
                get("/plotTsne", (registry.get(RAGApi.class)).getTsnePlot);
                get("/sentenceEmbeddings", (registry.get(RAGApi.class)).getSentenceEmbeddings);
            });
        });
    }
}
