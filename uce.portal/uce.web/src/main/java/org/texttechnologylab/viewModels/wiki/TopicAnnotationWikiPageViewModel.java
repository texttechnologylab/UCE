package org.texttechnologylab.viewModels.wiki;

import org.texttechnologylab.models.corpus.Corpus;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.corpus.Page;
import org.texttechnologylab.models.corpus.TopicDistribution;
import org.texttechnologylab.models.viewModels.CorpusViewModel;
import org.texttechnologylab.viewModels.ViewModelBase;

public class TopicAnnotationWikiPageViewModel extends ViewModelBase {
    private TopicDistribution topicDistribution;
    private String type;
    private CorpusViewModel corpus;
    /**
     * The Document this topic dist belongs to.
     */
    private Document document;

    /**
     * Can be null. Not every topic dist belongs to a single page.
     */
    private Page page;

    public TopicAnnotationWikiPageViewModel(){}

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

    public TopicDistribution getTopicDistribution() {
        return topicDistribution;
    }

    public void setTopicDistribution(TopicDistribution topicDistribution) {
        this.topicDistribution = topicDistribution;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
