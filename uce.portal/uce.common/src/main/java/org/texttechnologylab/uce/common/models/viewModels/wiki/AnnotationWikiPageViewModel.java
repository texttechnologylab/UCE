package org.texttechnologylab.uce.common.models.viewModels.wiki;

import org.texttechnologylab.uce.common.models.WikiModel;
import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.common.models.corpus.Page;
import org.texttechnologylab.uce.common.models.viewModels.CorpusViewModel;
import org.texttechnologylab.uce.common.states.KeywordInContextState;

public class AnnotationWikiPageViewModel extends ViewModelBase {
    private String coveredText;
    private WikiModel wikiModel;
    private CorpusViewModel corpus;
    private KeywordInContextState kwicState;
    /**
     * The Document this annotation belongs to.
     */
    private Document document;
    /**
     * Can be null. Not every annotation belongs to a single page.
     */
    private Page page;
    private String annotationType;

    public AnnotationWikiPageViewModel(){}

    public WikiModel getWikiModel() {
        return wikiModel;
    }

    public void setWikiModel(WikiModel wikiModel) {
        this.wikiModel = wikiModel;
    }

    public String getCoveredText() {
        return coveredText;
    }

    public void setCoveredText(String coveredText) {
        this.coveredText = coveredText;
    }

    public CorpusViewModel getCorpus() {
        return corpus;
    }

    public void setCorpus(CorpusViewModel corpus) {
        this.corpus = corpus;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public KeywordInContextState getKwicState() {
        return kwicState;
    }

    public void setKwicState(KeywordInContextState kwicState) {
        this.kwicState = kwicState;
    }

    public String getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(String annotationType) {
        this.annotationType = annotationType;
    }
}
