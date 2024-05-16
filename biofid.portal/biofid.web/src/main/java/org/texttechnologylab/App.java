package org.texttechnologylab;

import freemarker.template.Configuration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.texttechnologylab.config.SpringConfig;
import org.texttechnologylab.freeMarker.RequestContextHolder;
import org.texttechnologylab.routes.DocumentApi;
import org.texttechnologylab.routes.RAGApi;
import org.texttechnologylab.routes.SearchApi;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import spark.ExceptionHandler;
import spark.ModelAndView;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import static spark.Spark.*;

/**
 * Hello world!
 */
public class App {

    // Freemaker configuration
    private static final Configuration configuration = Configuration.getDefaultConfiguration();

    public static void main(String[] args) throws URISyntaxException, IOException {
        // Application context for services
        var context = new AnnotationConfigApplicationContext(SpringConfig.class);

        // Load in and test the language translation objects to handle multiple languages
        var languageResource = new LanguageResources("de-DE");
        var test = languageResource.get("Template");

        // Set the folder for our template files of freemaker
        try {
            configuration.setDirectoryForTemplateLoading(new File("resources/templates/"));
            staticFiles.location("/public");
        } catch (IOException e) {
            e.printStackTrace();
        }

        initSparkRoutes(context);
    }

    private static void initSparkRoutes(ApplicationContext context) {

        var searchApi = new SearchApi(context, configuration);
        var documentApi = new DocumentApi(context, configuration);
        var ragApi = new RAGApi(context, configuration);

        before((request, response) -> {
            // Check if the request contains a language parameter
            var languageResources = LanguageResources.fromRequest(request);
            response.header("Content-Language", languageResources.getDefaultLanguage());
            RequestContextHolder.setLanguageResources(languageResources);
        });

        // Landing page
        get("/", (request, response) -> {
            var model = new HashMap<String, Object>();
            model.put("title", "BioFID Portal");
            model.put("corpora", context.getBean(PostgresqlDataInterface_Impl.class).getAllCorpora());

            // The vm files are located under the resources directory
            return new ModelAndView(model, "index.ftl");
        }, new CustomFreeMarkerEngine(configuration));

        // A document reader view
        get("/documentReader", documentApi.getSingleDocumentReadView);

        // A corpus World View
        get("/globe", documentApi.getCorpusWorldView);

        // Gets a corpus inspector view
        get("/corpus", documentApi.getCorpusInspectorView);

        // Define default exception handler. This shows an error view then in the body.
        ExceptionHandler<Exception> defaultExceptionHandler = (exception, request, response) -> {
            response.status(500);
            response.body(new CustomFreeMarkerEngine(configuration).render(new ModelAndView(null, "defaultError.ftl")));
        };

        // API routes
        path("/api", () -> {

            exception(Exception.class, defaultExceptionHandler);

            before("/*", (q, a) -> System.out.println("Received API call."));

            path("/search", () -> {
                post("/default", searchApi.search);
                post("/semanticRole", searchApi.semanticRoleSearch);
                get("/active/page", searchApi.activeSearchPage);
                get("/active/sort", searchApi.activeSearchSort);
                get("/semanticRole/builder", searchApi.getSemanticRoleBuilderView);
            });

            path("/document", () -> {
                get("/reader/pagesList", documentApi.getPagesListView);
            });

            path("/rag", () -> {
                get("/new", ragApi.getNewRAGChat);
                post("/postUserMessage", ragApi.postUserMessage);
            });
        });
    }
}
