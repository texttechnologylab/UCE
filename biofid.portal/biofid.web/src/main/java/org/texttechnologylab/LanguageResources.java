package org.texttechnologylab;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.bson.Document;
import org.texttechnologylab.utils.SupportedLanguages;
import spark.Request;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class LanguageResources {

    private final Document languageTranslations;
    private final String defaultLanguage;
    private SupportedLanguages supportedLanguage;

    public LanguageResources(String defaultLanguage) throws URISyntaxException, IOException {
        this.defaultLanguage = defaultLanguage;
        switch(defaultLanguage){
            case "de-DE":
                supportedLanguage = SupportedLanguages.GERMAN;
                break;
            case "en-EN":
                supportedLanguage = SupportedLanguages.ENGLISH;
                break;
            default:
                supportedLanguage = SupportedLanguages.GERMAN;
                break;
        }
        var jsonData = Files.readString(Paths.get(LanguageResources.class.getClassLoader().getResource("languageTranslations.json").toURI()));
        var gson = new Gson();
        languageTranslations = gson.fromJson(jsonData, Document.class);
    }

    /**
     * Builds a language resource object with the correct language from a request
     * @param request
     */
    public static LanguageResources fromRequest(Request request) throws URISyntaxException, IOException {
        var language = request.cookie("language");
        var languageResources = new LanguageResources("de-DE"); // German is standard
        if (language != null && !language.equals("undefined")) {
            var langCode = language;
            // Sometimes the language is sent through a weird string with much more text. We just want the lang code then.
            if(language.length() > 6){
                langCode = language.split(";")[0].split(",")[1];
            }
            // Set the language in the session through a language object
            languageResources = new LanguageResources(langCode);
        }
        return languageResources;
    }

    public SupportedLanguages getSupportedLanguage() {return supportedLanguage;}

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
