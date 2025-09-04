package org.texttechnologylab.uce.web.auth;

import io.javalin.Javalin;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.texttechnologylab.uce.common.annotations.auth.Authentication;
import org.texttechnologylab.uce.web.freeMarker.Renderer;
import org.texttechnologylab.uce.web.routes.UceApi;

import java.util.HashMap;
import java.util.Map;


public class AuthenticationRouteRegister {

    private static final Logger logger = LogManager.getLogger(AuthenticationRouteRegister.class);

     /**
     * Registers all UceApi instances with annotated routes.
     *
     * @param apiInstances a map of UceApi classes to their manually constructed instances
     */
    public static void registerApis(Map<Class<? extends UceApi>, UceApi> apiInstances, Javalin javalinApp) {
        for (Map.Entry<Class<? extends UceApi>, UceApi> entry : apiInstances.entrySet()) {
            UceApi instance = entry.getValue();
            try {
                register(instance, javalinApp);
            } catch (Exception ex) {
                logger.error("[CRITICAL RISK] Failed to register a route for authentication: ", ex);
            }
        }
    }

    /**
     * Registers a single UceApi object with authentication rules through the AuthenticationAnnotation.
     */
    public static void register(UceApi api, Javalin javalinApp) {
        for (var field : api.getClass().getDeclaredFields()) {
            if (Handler.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    Handler originalRoute = (Handler) field.get(api);

                    var auth = field.getAnnotation(Authentication.class);
                    if (auth == null) {
                        continue;
                    }

                    String path = auth.path();
                    Handler wrappedRoute = originalRoute;

                    if (auth.required() == Authentication.Requirement.LOGGED_IN) {
                        wrappedRoute = ctx -> {
                            if (ctx.sessionAttribute("uceUser") == null) {
                                var model = new HashMap<String, Object>();
                                model.put("information", "You need to be logged in.");

                                ctx.status(401);
                                ctx.render(Renderer.renderToHTML("*/accessDenied.ftl", model));
                                return;
                            }
                            originalRoute.handle(ctx);
                        };
                    }

                    switch (auth.route()) {
                        case GET -> javalinApp.get(path, wrappedRoute);
                        case POST -> javalinApp.post(path, wrappedRoute);
                        case PUT -> javalinApp.put(path, wrappedRoute);
                        case DELETE -> javalinApp.delete(path, wrappedRoute);
                        case PATCH -> javalinApp.patch(path, wrappedRoute);
                        default -> throw new IllegalArgumentException("Unsupported HTTP method");
                    }

                } catch (Exception ex) {
                    logger.error("[SECURITY RISK] Failed to register a route for authentication: ", ex);
                }
            }
        }
    }
}


