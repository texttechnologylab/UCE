package org.texttechnologylab.config;

import com.google.gson.Gson;
import org.texttechnologylab.config.corpusConfig.CorpusAnnotationConfig;
import org.texttechnologylab.config.corpusConfig.OtherConfig;

public class CorpusConfig {
    private String name;
    private String author;
    private String language;
    private CorpusAnnotationConfig annotations;
    private OtherConfig other;

    public static CorpusConfig fromJson(String corpusConfigJson){
        var gson = new Gson();
        var config = gson.fromJson(corpusConfigJson, CorpusConfig.class);
        return config;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public CorpusAnnotationConfig getAnnotations() {
        return annotations;
    }

    public void setAnnotations(CorpusAnnotationConfig annotations) {
        this.annotations = annotations;
    }

    public OtherConfig getOther() {
        return other;
    }

    public void setOther(OtherConfig other) {
        this.other = other;
    }
}

