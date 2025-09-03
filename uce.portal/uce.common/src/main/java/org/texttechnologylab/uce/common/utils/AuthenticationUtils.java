package org.texttechnologylab.uce.common.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class AuthenticationUtils {

    public static PublicKey getKey(String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        var byteKey = Base64.getDecoder().decode(key.getBytes());
        var X509publicKey = new X509EncodedKeySpec(byteKey);
        var kf = KeyFactory.getInstance("RSA");

        return kf.generatePublic(X509publicKey);
    }

    public static JsonObject parseIdToken(String idToken){
        String[] parts = idToken.split("\\.");
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return JsonParser.parseString(payloadJson).getAsJsonObject();
    }
}
