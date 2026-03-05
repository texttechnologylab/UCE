package org.texttechnologylab.uce.common.config.uceConfig;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthConfig {
    private boolean isActivated;
    private String publicUrl;
    private String redirectUrl;

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = normalizeUrlBase(publicUrl);
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = normalizeUrlBase(redirectUrl);
    }

    public static String normalizeUrlBase(String url) {
        if (url == null) {
            return null;
        }
        var normalized = url.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
