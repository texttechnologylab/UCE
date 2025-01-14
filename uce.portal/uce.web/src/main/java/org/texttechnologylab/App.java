package org.texttechnologylab;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simpleframework.xml.transform.InvalidFormatException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.config.SpringConfig;
import org.texttechnologylab.config.UceConfig;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.freeMarker.RequestContextHolder;
import org.texttechnologylab.models.corpus.Corpus;
import org.texttechnologylab.models.corpus.UCELog;
import org.texttechnologylab.routes.*;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.utils.ImageUtils;
import org.texttechnologylab.utils.SystemStatus;
import spark.ExceptionHandler;
import spark.ModelAndView;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.UUID;

import static spark.Spark.*;

/**
 * Hello world!
 */
public class App {
    private static final Configuration configuration = Configuration.getDefaultConfiguration();
    private static final Logger logger = LogManager.getLogger();
    private static CommonConfig commonConfig = null;

    public static void main(String[] args) throws IOException {

        logger.info("Starting the UCE web service...");

        logger.info("Parsing the UCE config...");
        try {
            parseCommandLine(args);
            logger.info("UCE Config read and instantiated.");
        } catch (MissingOptionException ex) {
            logger.error("UCE couldn't parse the UceConfig in the CLI properly. " +
                    "UCE will still start with a default config, but this is not a desirable state.", ex);
        } catch (Exception ex){
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
        var context = new AnnotationConfigApplicationContext(SpringConfig.class);
        logger.info("Loaded application context and services.");

        // Load in and test the language translation objects to handle multiple languages
        logger.info("Testing the language resources:");
        var languageResource = new LanguageResources("de-DE");
        logger.info(languageResource.get("search"));

        // Start the different cronjobs in the background
        SessionManager.InitSessionManager(commonConfig.getSessionJobInterval());
        logger.info("Initialized the Session Job.");

        SystemStatus.InitSystemStatus(commonConfig.getSystemJobInterval(), context);
        logger.info("Initialized the System Job.");

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
        lines.set(3, "    --secondary: " +  SystemStatus.UceConfig.getCorporate().getSecondaryColor() + ";");
        Files.write(siteCss.toPath(), lines, StandardOpenOption.TRUNCATE_EXISTING);

        // Logo
        SystemStatus.UceConfig.getCorporate().setLogo(convertConfigImageString(SystemStatus.UceConfig.getCorporate().getLogo()));

        // Team Members
        for(var member: SystemStatus.UceConfig.getCorporate().getTeam().getMembers())
            member.setImage(convertConfigImageString(member.getImage()));
    }

    /**
     * Converts a img string in the config to a proper usable base64encoded image
     * @param imgString
     * @return
     * @throws IOException
     * @throws InvalidFormatException
     */
    private static String convertConfigImageString(String imgString) throws Exception {
        if(imgString.startsWith("BASE64::")){
            // If the logo is a base64 string, we only need to remove the prefix
            return imgString.replace("BASE64::", "");
        } else if (imgString.startsWith("FILE::")){
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

        var parser = new DefaultParser();
        var gson = new Gson();

        var cmd = parser.parse(options, args);
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

    private static void initSparkRoutes(ApplicationContext context) {

        var searchApi = new SearchApi(context, configuration);
        var documentApi = new DocumentApi(context, configuration);
        var ragApi = new RAGApi(context, configuration);
        var corpusUniverseApi = new CorpusUniverseApi(context, configuration);
        var wikiApi = new WikiApi(context, configuration);

        before((request, response) -> {
            // Setup and log all API calls with some information.
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

            // Check if the request contains a language parameter
            var languageResources = LanguageResources.fromRequest(request);
            response.header("Content-Language", languageResources.getDefaultLanguage());
            RequestContextHolder.setLanguageResources(languageResources);
        });

        // Landing page
        get("/", (request, response) -> {
            var model = new HashMap<String, Object>();
            model.put("title", SystemStatus.UceConfig.getMeta().getName());
            model.put("corpora", context.getBean(PostgresqlDataInterface_Impl.class)
                    .getAllCorpora()
                    .stream().map(Corpus::getViewModel)
                    .toList());
            model.put("system", SystemStatus.UceConfig);
            model.put("isSparqlAlive", SystemStatus.JenaSparqlStatus.isAlive());
            model.put("isDbAlive", SystemStatus.PostgresqlDbStatus.isAlive());
            model.put("isRagAlive", SystemStatus.RagServiceStatus.isAlive());

            // The vm files are located under the resources directory
            return new ModelAndView(model, "index.ftl");
        }, new CustomFreeMarkerEngine(configuration));

        // A document reader view
        get("/documentReader", documentApi.getSingleDocumentReadView);

        // A corpus World View
        get("/globe", documentApi.get3dGlobe);

        // Define default exception handler. This shows an error view then in the body.
        ExceptionHandler<Exception> defaultExceptionHandler = (exception, request, response) -> {
            logger.error("Unknown error handled in API - returning default error view.", exception);
            response.status(500);
            response.body(new CustomFreeMarkerEngine(configuration).render(new ModelAndView(null, "defaultError.ftl")));
        };

        // API routes
        path("/api", () -> {

            exception(Exception.class, defaultExceptionHandler);

            before("/*", (req, res) -> {
            });

            path("/wiki", () -> {
                get("/annotationPage", wikiApi.getAnnotationPage);
                post("/queryOntology", wikiApi.queryOntology);
            });

            path("/corpus", () -> {
                get("/inspector", documentApi.getCorpusInspectorView);
                get("/documentsList", documentApi.getDocumentListOfCorpus);
            });

            path("/search", () -> {
                post("/default", searchApi.search);
                post("/semanticRole", searchApi.semanticRoleSearch);
                get("/active/page", searchApi.activeSearchPage);
                get("/active/sort", searchApi.activeSearchSort);
                get("/semanticRole/builder", searchApi.getSemanticRoleBuilderView);
            });

            path("/corpusUniverse", () -> {
                // Gets a corpus universe view
                get("/new", corpusUniverseApi.getCorpusUniverseView);
                post("/fromSearch", corpusUniverseApi.fromSearch);
                post("/fromCorpus", corpusUniverseApi.fromCorpus);
                get("/nodeInspectorContent", corpusUniverseApi.getNodeInspectorContentView);
            });

            path("/document", () -> {
                get("/reader/pagesList", documentApi.getPagesListView);
            });

            path("/rag", () -> {
                get("/new", ragApi.getNewRAGChat);
                post("/postUserMessage", ragApi.postUserMessage);
                get("/plotTsne", ragApi.getTsnePlot);
            });
        });
    }
}
