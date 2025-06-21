package org.texttechnologylab.freeMarker;

import org.texttechnologylab.LanguageResources;
import org.texttechnologylab.config.UceConfig;
import org.texttechnologylab.models.authentication.UceUser;
import org.texttechnologylab.utils.SystemStatus;

/**
 * A class which can hold any objects we want to keep track of within a FreeMarker session cycle.
 */
public class RequestContextHolder {

    private static final ThreadLocal<LanguageResources> languageResourcesHolder = new ThreadLocal<>();
    private static final ThreadLocal<UceUser> authenticatedUceUserHolder = new ThreadLocal<>();
    private static final ThreadLocal<UceConfig> uceConfigHolder = new ThreadLocal<>();

    public static void setLanguageResources(LanguageResources languageResources) {
        languageResourcesHolder.set(languageResources);
    }

    public static void setUceConfigHolder(UceConfig uceConfig) {
        uceConfigHolder.set(uceConfig);
    }

    public static void setAuthenticatedUceUser(UceUser uceUser) {
        authenticatedUceUserHolder.set(uceUser);
    }

    public static LanguageResources getLanguageResources() {
        return languageResourcesHolder.get();
    }

    public static UceUser getAuthenticatedUceUser() {
        return authenticatedUceUserHolder.get();
    }

    public static UceConfig getUceConfig() {
        return uceConfigHolder.get();
    }

    public static void remove() {
        languageResourcesHolder.remove();
        authenticatedUceUserHolder.remove();
        uceConfigHolder.remove();
    }
}
