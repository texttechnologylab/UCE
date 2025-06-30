package org.texttechnologylab.routes;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import freemarker.template.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.CustomFreeMarkerEngine;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.freeMarker.RequestContextHolder;
import org.texttechnologylab.models.authentication.UceUser;
import org.texttechnologylab.services.AuthenticationService;
import org.texttechnologylab.utils.AuthenticationUtils;
import org.texttechnologylab.utils.SystemStatus;
import spark.ModelAndView;
import spark.Route;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class AuthenticationApi implements UceApi {

    private static final CommonConfig commonConfig = new CommonConfig();
    private static final Logger logger = LogManager.getLogger(AuthenticationApi.class);
    private Configuration freemarkerConfig;
    private AuthenticationService authenticationService;
    private final Gson gson = new Gson();

    public AuthenticationApi(ApplicationContext serviceContext, Configuration freemarkerConfig){
        this.authenticationService = serviceContext.getBean(AuthenticationService.class);
        this.freemarkerConfig = freemarkerConfig;
    }

    /**
     * Gets called when the user logs out of the authentication. We clear and invalidate the session then.
     */
    public Route logoutCallback = ((request, response) -> {
        request.session().attribute("uceUser", null);
        request.session().invalidate();
        response.redirect("/");
        return null;
    });

    /**
     * A route that gets called by the keycloak server AFTER having a user logged in. In this request, we get a
     * code from keycloak which isn't yet a token. We need to again ask keycloak to send us, based on that code, the final
     * jtw token which then has all the claims of that user and which we can then work with.
     */
    public Route loginCallback = ((request, response) -> {
        try{
            var code = request.queryParams("code");
            if (code == null) {
                return "Missing authorization parameter 'code' - cannot verify login!";
            }

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
                return "Error retrieving token: " + result;
            }

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
                user.setEmailVerified(Boolean.parseBoolean(parsedIdToken.get("email_verified").toString().replace("\"", "")));
                user.setName(parsedIdToken.get("name").toString().replace("\"", ""));
                user.setEmail(parsedIdToken.get("email").toString().replace("\"", ""));
                user.setUsername(parsedIdToken.get("preferred_username").toString().replace("\"", ""));
            }

            var refreshToken = tokenResponse.has("refresh_token") ? tokenResponse.get("refresh_token").getAsString().replace("\"", "") : null;
            user.setRefreshToken(refreshToken);
            var expiresIn = tokenResponse.get("expires_in").getAsLong();
            user.setExpiresIn(expiresIn);

            request.session().attribute("uceUser", user);

            // We redirect back to the main page after logging in.
            response.redirect("/");
            return null;
        } catch (Exception ex){
            logger.error("Error in the authentication: a callback produced an error. Request " +
                         "with id=" + request.attribute("id") + " to this endpoint for URI parameters.", ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

}
