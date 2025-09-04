package org.texttechnologylab.uce.common.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.keycloak.authorization.client.AuthzClient;
import org.springframework.stereotype.Service;
import org.texttechnologylab.uce.common.config.CommonConfig;
import org.texttechnologylab.uce.common.models.util.HealthStatus;
import org.texttechnologylab.uce.common.utils.AuthenticationUtils;
import org.texttechnologylab.uce.common.utils.SystemStatus;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

@Service
public class AuthenticationService implements Authentication{

    private static final CommonConfig commonConfig = new CommonConfig();
    // Good resources to work with keycloak: https://www.keycloak.org/securing-apps/authz-client
    private AuthzClient authzClient;

    public AuthenticationService(){
        TestConnection();
    }

    public void TestConnection(){
        if(SystemStatus.UceConfig != null && !SystemStatus.UceConfig.authIsEnabled()){
            SystemStatus.AuthenticationService = new HealthStatus(false, "Auth is disabled in this instance.", null);
            return;
        }

        try{
            this.authzClient = AuthzClient.create(commonConfig.getKeyCloakConfiguration());
            SystemStatus.AuthenticationService = new HealthStatus(true, "Connection successful.", null);
        } catch (Exception ex){
            SystemStatus.AuthenticationService = new HealthStatus(false, "Unable to connect due to error: ", ex);
        }
    }

    public boolean hasPermission(String token, String resource, String scope){
        return true;
    }

    private Claims getClaims(String token) throws NoSuchAlgorithmException, InvalidKeySpecException {
        var publicKey = AuthenticationUtils.getKey("");
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
