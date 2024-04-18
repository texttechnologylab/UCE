package org.texttechnologylab.freeMarker;

import org.texttechnologylab.LanguageResources;

/**
 * A class which can hold any objects we want to keep track of within a FreeMarker session cycle.
 */
public class RequestContextHolder {

    private static final ThreadLocal<LanguageResources> languageResourcesHolder = new ThreadLocal<>();

    public static void setLanguageResources(LanguageResources languageResources) {
        languageResourcesHolder.set(languageResources);
    }

    public static LanguageResources getLanguageResources() {
        return languageResourcesHolder.get();
    }

    public static void remove() {
        languageResourcesHolder.remove();
    }

}
