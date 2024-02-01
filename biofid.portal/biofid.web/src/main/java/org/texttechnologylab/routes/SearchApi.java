package org.texttechnologylab.routes;

import spark.Route;

public class SearchApi {

    public static Route search = ((request, response) -> {
        return "Default Searching";
    });
}
