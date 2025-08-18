package org.texttechnologylab.config;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.config.corpusConfig.CorpusAnnotationConfig;
import org.texttechnologylab.config.corpusConfig.OtherConfig;

import javax.persistence.Column;

@Getter
@Setter
public class CorpusConfig {
    private String name;
    private String author;
    private String language;
    private String description;
    private CorpusAnnotationConfig annotations;
    private boolean addToExistingCorpus;
    private OtherConfig other;

    public static CorpusConfig fromJson(String corpusConfigJson){
        var gson = new Gson();
        var config = gson.fromJson(corpusConfigJson, CorpusConfig.class);
        return config;
    }
}

