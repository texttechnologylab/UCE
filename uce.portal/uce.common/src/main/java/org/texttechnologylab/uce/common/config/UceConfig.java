package org.texttechnologylab.uce.common.config;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;

import org.texttechnologylab.uce.common.config.uceConfig.AuthConfig;
import org.texttechnologylab.uce.common.config.uceConfig.CorporateConfig;
import org.texttechnologylab.uce.common.config.uceConfig.MetaConfig;
import org.texttechnologylab.uce.common.config.uceConfig.SettingsConfig;

@Getter
@Setter
public class UceConfig {
    private MetaConfig meta;
    private CorporateConfig corporate;
    private SettingsConfig settings;

    public boolean authIsEnabled(){
        return settings.getAuthentication().isActivated();
    }

    public static UceConfig fromJson(String uceConfigJson){
        if (uceConfigJson == null || uceConfigJson.isBlank()) {
            return null;
        }
        var gson = new Gson();
        var config = gson.fromJson(uceConfigJson, UceConfig.class);
        if (config != null) {
            config.normalize();
            config.applyEnvironmentOverrides();
        }
        return config;
    }

    public void applyEnvironmentOverrides() {
        if (settings == null) {
            return;
        }

        var port = System.getenv("UCE_PORT");
        if (port != null && !port.isBlank()) {
            try {
                settings.setPort(Integer.parseInt(port));
            } catch (NumberFormatException ignored) {
                // Keep the existing port value if parsing fails.
            }
        }

        var auth = settings.getAuthentication();
        if (auth != null) {
            var authEnabled = System.getenv("UCE_AUTH_ENABLED");
            if (authEnabled != null && !authEnabled.isBlank()) {
                auth.setActivated(Boolean.parseBoolean(authEnabled));
            }

            var authPublicUrl = System.getenv("UCE_AUTH_PUBLIC_URL");
            if (authPublicUrl != null && !authPublicUrl.isBlank()) {
                auth.setPublicUrl(authPublicUrl);
            }

            var authRedirectUrl = System.getenv("UCE_AUTH_REDIRECT_URL");
            if (authRedirectUrl != null && !authRedirectUrl.isBlank()) {
                auth.setRedirectUrl(authRedirectUrl);
            }
        }
    }

    public void normalize() {
        if (settings == null) {
            return;
        }
        var auth = settings.getAuthentication();
        if (auth != null) {
            auth.setPublicUrl(AuthConfig.normalizeUrlBase(auth.getPublicUrl()));
            auth.setRedirectUrl(AuthConfig.normalizeUrlBase(auth.getRedirectUrl()));
        }
    }

    public UceConfig(){}
}
