package org.texttechnologylab.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.texttechnologylab.annotations.auth.Authentication;
import org.texttechnologylab.routes.UceApi;
import spark.Route;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.halt;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.get;
import static spark.Spark.delete;
import static spark.Spark.patch;

public class AuthenticationRouteRegister {

    private static final Logger logger = LogManager.getLogger(AuthenticationRouteRegister.class);

     /**
     * Registers all UceApi instances with annotated routes.
     *
     * @param apiInstances a map of UceApi classes to their manually constructed instances
     */
    public static void registerApis(Map<Class<? extends UceApi>, UceApi> apiInstances) {
        for (Map.Entry<Class<? extends UceApi>, UceApi> entry : apiInstances.entrySet()) {
            Class<? extends UceApi> clazz = entry.getKey();
            UceApi instance = entry.getValue();

            try {
                register(instance);
            } catch (Exception ex) {
                logger.error("[CRITICAL RISK] Failed to register a route for authentication: ", ex);
            }
        }
    }

    /**
     * Registers a single UceApi object with authentication rules through the AuthenticationAnnotation.
     */
    public static void register(UceApi api) {
        for (var field : api.getClass().getDeclaredFields()) {
            if (Route.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    Route originalRoute = (Route) field.get(api);

                    var auth = field.getAnnotation(Authentication.class);
                    if (auth == null) {
                        continue;
                    }

                    String path = auth.path();
                    Route wrappedRoute = originalRoute;

                    if (auth.required() == Authentication.Requirement.LOGGED_IN) {
                        wrappedRoute = (req, res) -> {
                            if (req.session().attribute("uceUser") == null) {
                                halt(401, "Unauthorized – Please log in");
                            }
                            return originalRoute.handle(req, res);
                        };
                    }

                    switch (auth.route()) {
                        case GET -> get(path, wrappedRoute);
                        case POST -> post(path, wrappedRoute);
                        case PUT -> put(path, wrappedRoute);
                        case DELETE -> delete(path, wrappedRoute);
                        case PATCH -> patch(path, wrappedRoute);
                        default -> throw new IllegalArgumentException("Unsupported HTTP method");
                    }

                } catch (Exception ex) {
                    logger.error("[SECURITY RISK] Failed to register a route for authentication: ", ex);
                }
            }
        }
    }
}


