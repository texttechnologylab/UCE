package org.texttechnologylab.uce.common.models.viewModels;

import org.texttechnologylab.uce.common.config.CorpusConfig;
import org.texttechnologylab.uce.common.models.corpus.Corpus;

public class CorpusViewModel {

    private Corpus corpus;
    private CorpusConfig corpusConfig;

    public CorpusViewModel(){

    }
    public CorpusViewModel(Corpus corpus, String corpusConfig){
        this.corpus = corpus;
        this.corpusConfig = CorpusConfig.fromJson(corpusConfig);
    }

    public Corpus getCorpus() {
        return corpus;
    }

    public void setCorpus(Corpus corpus) {
        this.corpus = corpus;
    }

    public CorpusConfig getCorpusConfig() {
        return corpusConfig;
    }

    public void setCorpusConfig(CorpusConfig corpusConfig) {
        this.corpusConfig = corpusConfig;
    }
}
