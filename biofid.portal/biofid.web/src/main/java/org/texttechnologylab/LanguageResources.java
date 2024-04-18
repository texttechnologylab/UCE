package org.texttechnologylab;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.bson.Document;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class LanguageResources {

    private final Document languageTranslations;
    private final String defaultLanguage;

    public LanguageResources(String defaultLanguage) throws URISyntaxException, IOException {
        this.defaultLanguage = defaultLanguage;
        var jsonData = Files.readString(Paths.get(LanguageResources.class.getClassLoader().getResource("languageTranslations.json").toURI()));
        var gson = new Gson();
        languageTranslations = gson.fromJson(jsonData, Document.class);
    }

    public String getDefaultLanguage(){
        return defaultLanguage;
    }

    public String get(String resourceName){
        return get(resourceName, defaultLanguage);
    }

    public String get(String resourceName, String lang) {
        return languageTranslations.get(resourceName, LinkedTreeMap.class).get(lang).toString();
    }

}
