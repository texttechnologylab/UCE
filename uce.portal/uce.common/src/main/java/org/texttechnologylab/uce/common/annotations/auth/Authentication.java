package org.texttechnologylab.uce.common.annotations.auth;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Authentication {

    Requirement required() default Requirement.NONE;
    RouteTypes route() default RouteTypes.GET;
    String path(); // full path like "/api/rag/postUserMessage"

    enum Requirement {
        LOGGED_IN,
        NONE
    }

    enum RouteTypes{
        POST,
        GET,
        PUT,
        DELETE,
        PATCH
    }

}
