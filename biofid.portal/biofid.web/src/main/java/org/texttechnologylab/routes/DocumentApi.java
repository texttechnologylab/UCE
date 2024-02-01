package org.texttechnologylab.routes;

import spark.Route;

public class DocumentApi {

    public static Route getSingleDocument = ((request, response) -> {
        return "Get a single document";
    });
}
