package org.texttechnologylab;

import freemarker.template.Configuration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.texttechnologylab.config.SpringConfig;
import org.texttechnologylab.routes.DocumentApi;
import org.texttechnologylab.routes.SearchApi;
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

        var searchApi = new SearchApi(context);
        var documentApi = new DocumentApi(context);

        // Landing page
        get("/", (request, response) -> {
            var model = new HashMap<String, Object>();
            model.put("title", "BioFID Portal");

            // The vm files are located under the resources directory
            return new ModelAndView(model, "index.ftl");
        }, new FreeMarkerEngine(configuration));

        // API routes
        path("/api", () -> {

            before("/*", (q, a) -> System.out.println("Received API call."));

            path("/search", () -> {
                get("/default", searchApi.search);
            });

            path("/document", () -> {
                get("/single", documentApi.getSingleDocument);
            });

        });
    }
}
