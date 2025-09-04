package org.texttechnologylab.uce.search.utils;

import java.util.ArrayList;
import java.util.List;

public final class Stopwords {
    public static List<String> GermanStopwords = null;
    public static List<String> EnglishStopwords = null;
    public static List<String> GetStopwords(String languageCode){
        if(languageCode.equals("de-DE")) return GermanStopwords;
        else if(languageCode.equals("en-EN")) return EnglishStopwords;
        else return new ArrayList<>();
    }
    public static void SetStopwords(String languageCode, List<String> stopwords){
        if(languageCode.equals("de-DE")) GermanStopwords = stopwords;
        else if(languageCode.equals("en-EN")) EnglishStopwords = stopwords;
    }
}
