package org.texttechnologylab;

import freemarker.template.Configuration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.texttechnologylab.config.HibernateConf;
import org.texttechnologylab.config.SpringConfig;
import org.texttechnologylab.routes.DocumentApi;
import org.texttechnologylab.routes.SearchApi;
import org.texttechnologylab.services.DatabaseService;
import spark.ExceptionHandler;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static spark.Spark.*;

/**
 * Hello world!
 */
public class App {

    // Freemaker configuration
    private static final Configuration configuration = Configuration.getDefaultConfiguration();

    public static void main(String[] args) {
        // Application context for services
        var context = new AnnotationConfigApplicationContext(SpringConfig.class);

        // Set the folder for our template files of freemaker
        try {
            configuration.setDirectoryForTemplateLoading(new File("resources/templates/"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        initSparkRoutes(context);
    }

    private static void initSparkRoutes(ApplicationContext context) {

        var searchApi = new SearchApi(context, configuration);
        var documentApi = new DocumentApi(context, configuration);

        // Landing page
        get("/", (request, response) -> {
            var model = new HashMap<String, Object>();
            model.put("title", "BioFID Portal");
            model.put("corpora", context.getBean(DatabaseService.class).getAllCorpora());

            // The vm files are located under the resources directory
            return new ModelAndView(model, "index.ftl");
        }, new FreeMarkerEngine(configuration));

        // A document reader view
        get("/documentReader", documentApi.getSingleDocumentReadView);

        // A corpus World View
        get("/globe", documentApi.getCorpusWorldView);

        // Define default exception handler. This shows an error view then in the body.
        ExceptionHandler<Exception> defaultExceptionHandler = (exception, request, response) -> {
            response.status(500);
            response.body(new FreeMarkerEngine(configuration).render(new ModelAndView(null, "defaultError.ftl")));
        };

        // API routes
        path("/api", () -> {

            exception(Exception.class, defaultExceptionHandler);

            before("/*", (q, a) -> System.out.println("Received API call."));

            path("/search", () -> {
                post("/default", searchApi.search);
                get("/active/page", searchApi.activeSearchPage);
                get("/active/sort", searchApi.activeSearchSort);
            });

            path("/document", () -> {
            });
        });
    }
}
