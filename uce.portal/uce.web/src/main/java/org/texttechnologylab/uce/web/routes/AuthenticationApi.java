package org.texttechnologylab.uce.web.routes;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import freemarker.template.Configuration;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.uce.common.config.CommonConfig;
import org.texttechnologylab.uce.common.models.authentication.UceUser;
import org.texttechnologylab.uce.common.security.DocumentAccessContext;
import org.texttechnologylab.uce.common.services.AuthenticationService;
import org.texttechnologylab.uce.common.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.uce.common.utils.AuthenticationUtils;
import org.texttechnologylab.uce.common.utils.SystemStatus;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

public class AuthenticationApi implements UceApi {

    private static final CommonConfig commonConfig = new CommonConfig();
    private static final Logger logger = LogManager.getLogger(AuthenticationApi.class);
    private Configuration freemarkerConfig;
    private AuthenticationService authenticationService;
    private PostgresqlDataInterface_Impl db;
    private final Gson gson = new Gson();

    public AuthenticationApi(ApplicationContext serviceContext, Configuration freemarkerConfig){
        this.authenticationService = serviceContext.getBean(AuthenticationService.class);
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.freemarkerConfig = freemarkerConfig;
    }

    /**
     * Gets called when the user logs out of the authentication. We clear and invalidate the session then.
     */
    public void logoutCallback(Context ctx) {
        ctx.sessionAttribute("uceUser", null);
        ctx.req().getSession().invalidate();
        ctx.redirect("/");
    };

    /**
     * Used by the frontend to periodically detect expired sessions without waiting for a user action.
     * Returns 204 if the session is still logged in, otherwise 401.
     */
    public void authPing(Context ctx) {
        ctx.header("Cache-Control", "no-store");
        if (ctx.sessionAttribute("uceUser") == null) {
            ctx.status(401);
            return;
        }
        ctx.status(204);
    }

    /**
     * A route that gets called by the keycloak server AFTER having a user logged in. In this request, we get a
     * code from keycloak which isn't yet a token. We need to again ask keycloak to send us, based on that code, the final
     * jtw token which then has all the claims of that user and which we can then work with.
     */
    public void loginCallback(Context ctx) {
        try{
            var code = ctx.queryParam("code");
            if (code == null) {
                ctx.result("Missing authorization parameter 'code' - cannot verify login!");
                return;
            }

            logger.info("Login callback received with authorization code (length={} chars)", code.length());

            var redirectUri = SystemStatus.UceConfig.getSettings().getAuthentication().getRedirectUrl();
            var clientId = commonConfig.getKeyCloakConfiguration().getResource();

            var urlParameters = "grant_type=authorization_code"
                                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                                + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                                + "&redirect_uri=" + URLEncoder.encode(redirectUri + "/login", StandardCharsets.UTF_8)
                                + "&client_secret=" + URLEncoder.encode(commonConfig.getKeyCloakConfiguration().getCredentials().get("secret").toString(), StandardCharsets.UTF_8);

            // Token endpoint
            var url = new URL(commonConfig.getKeyCloakConfiguration().getAuthServerUrl() + "/realms/uce/protocol/openid-connect/token");
            var conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            try (var os = conn.getOutputStream()) {
                os.write(urlParameters.getBytes(StandardCharsets.UTF_8));
            }

            var status = conn.getResponseCode();
            var is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
            var result = new BufferedReader(new InputStreamReader(is))
                    .lines().collect(Collectors.joining("\n"));
            if (status != 200) {
                ctx.result("Error retrieving token: " + result);
                return;
            }
            logger.debug("Token endpoint response received with HTTP status {}", status);

            // So the Authenticator sent us a couple of very useful information
            var gson = new Gson();
            var tokenResponse = gson.fromJson(result, JsonObject.class);
            var user = new UceUser();

            // The access token is like the bearer token - we store that in the session
            var accessToken = tokenResponse.get("access_token").getAsString();
            user.setAccessToken(accessToken);

            // The id token contains information about the user, such as names, email and such
            var idToken = tokenResponse.has("id_token") ? tokenResponse.get("id_token").getAsString() : null;
            if(idToken != null) {
                var parsedIdToken = AuthenticationUtils.parseIdToken(idToken);
                logger.info("ID token payload: {}", parsedIdToken);
                user.setEmailVerified(Boolean.parseBoolean(parsedIdToken.get("email_verified").toString().replace("\"", "")));
                user.setName(parsedIdToken.get("name").toString().replace("\"", ""));
                user.setEmail(parsedIdToken.get("email").toString().replace("\"", ""));
                user.setUsername(parsedIdToken.get("preferred_username").toString().replace("\"", ""));
                user.setRoles(java.util.EnumSet.noneOf(DocumentAccessContext.Role.class));
                var groupsFromIdToken = extractGroups(parsedIdToken);
                if (!groupsFromIdToken.isEmpty()) {
                    user.setGroups(groupsFromIdToken);
                    
                    // TODO Implement more fine-grained role assignment 
                    if (groupsFromIdToken.contains("uce-admin")) {
                        user.getRoles().add(DocumentAccessContext.Role.ADMIN);
                    }
                }

                logger.info("Parsed ID token for user={} with groups from ID token size={}", user.getUsername(), groupsFromIdToken.size());
                logger.info("User roles assigned: {}", user.getRoles());
            }

            var refreshToken = tokenResponse.has("refresh_token") ? tokenResponse.get("refresh_token").getAsString().replace("\"", "") : null;
            user.setRefreshToken(refreshToken);
            var expiresIn = tokenResponse.get("expires_in").getAsLong();
            user.setExpiresIn(expiresIn);

            // If groups weren't present in the ID token, try the access token (Keycloak may put them there)
            if (user.getGroups() == null || user.getGroups().isEmpty()) {
                var parsedAccessToken = AuthenticationUtils.parseIdToken(accessToken);
                logger.info("Access token payload: {}", parsedAccessToken);
                var groupsFromAccessToken = extractGroups(parsedAccessToken);
                if (!groupsFromAccessToken.isEmpty()) {
                    user.setGroups(groupsFromAccessToken);
                }
                logger.info("Groups from access token size={}", groupsFromAccessToken.size());
            }

            // Ensure groups is non-null for downstream permission calculation
            if (user.getGroups() == null) {
                user.setGroups(java.util.Collections.emptySet());
                logger.info("No groups found in either token; using empty group set");
            }

            // Ensure roles is non-null
            if (user.getRoles() == null) {
                user.setRoles(java.util.EnumSet.noneOf(DocumentAccessContext.Role.class));
                logger.info("No roles assigned; using empty role set");
            }

            ctx.sessionAttribute("uceUser", user);
            logger.info("Session user set for username={}, groups={}", user.getUsername(), user.getGroups());

            // Update the user's document permissions
            // TODO we should later do this asynchronously when group memberships change using the Keycloak API
            logger.info("Calculating effective permissions for username={} with groups size={}", user.getUsername(), user.getGroups().size());
            db.calculateEffectivePermissions(user.getUsername(), user.getGroups());

            // Redirect back to the calling page (OIDC `state`) after logging in.
            // Only allow local relative paths to prevent open redirects.
            var state = ctx.queryParam("state");
            if (state != null && state.startsWith("/") && !state.startsWith("//") && !state.contains("\r") && !state.contains("\n")) {
                ctx.redirect(state);
            } else {
                ctx.redirect("/");
            }
        } catch (Exception ex){
            logger.error("Error in the authentication: a callback produced an error. Request " +
                         "with id=" + ctx.attribute("id") + " to this endpoint for URI parameters.", ex);
            ctx.render("defaultError.ftl");
        }
    }

    private Set<String> extractGroups(JsonObject tokenPayload) {
        if (!tokenPayload.has("groups")) {
            return java.util.Collections.emptySet();
        }

        return tokenPayload
                .getAsJsonArray("groups")
                .asList()
                .stream()
                // group names start with a slash; strip it for compatibility with DB permissions
                .map(e -> e.getAsString().replaceFirst("^/", ""))
                .collect(Collectors.toSet());
    }

}
