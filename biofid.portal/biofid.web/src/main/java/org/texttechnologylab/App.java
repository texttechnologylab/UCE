package org.texttechnologylab;

import org.texttechnologylab.routes.DocumentApi;
import org.texttechnologylab.routes.SearchApi;

import static spark.Spark.*;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        initSparkRoutes();
    }

    private static void initSparkRoutes() {
        path("/api", () -> {

            before("/*", (q, a) -> System.out.println("Received API call."));

            path("/search", () -> {
                get("/default", SearchApi.search);
            });

            path("/document", () -> {
                get("/single", DocumentApi.getSingleDocument);
            });

        });
    }
}
