package org.texttechnologylab.uce.web;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import io.javalin.http.Context;
import org.bson.Document;
import org.texttechnologylab.uce.common.utils.SupportedLanguages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public final class LanguageResources {

    private final Document languageTranslations;
    private final String defaultLanguage;
    private SupportedLanguages supportedLanguage;

    public LanguageResources(String defaultLanguage) throws IOException {
        this.defaultLanguage = defaultLanguage;
        switch (defaultLanguage) {
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
        var inputStream = getClass().getClassLoader().getResourceAsStream("languageTranslations.json");
        String jsonData;
        try (var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            jsonData = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        var gson = new Gson();
        languageTranslations = gson.fromJson(jsonData, Document.class);
    }

    /**
     * Builds a language resource object with the correct language from a request
     *
     * @param ctx
     */
    public static LanguageResources fromRequest(Context ctx) throws IOException {
        var language = ctx.cookie("language");
        var languageResources = new LanguageResources("en-EN"); // English is standard
        if (language != null && !language.equals("undefined")) {
            var langCode = language;
            // Sometimes the language is sent through a weird string with much more text. We just want the lang code then.
            if (language.length() > 6) {
                langCode = language.split(";")[0].split(",")[1];
            }
            // Set the language in the session through a language object
            languageResources = new LanguageResources(langCode);
        }
        return languageResources;
    }

    public SupportedLanguages getSupportedLanguage() {
        return supportedLanguage;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public String get(String resourceName) {
        return get(resourceName, defaultLanguage);
    }

    public String get(String resourceName, String lang) {
        return languageTranslations.get(resourceName, LinkedTreeMap.class).get(lang).toString();
    }

}
