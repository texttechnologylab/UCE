package org.texttechnologylab.models.viewModels.wiki;

import org.texttechnologylab.models.corpus.UCEMetadata;
import org.texttechnologylab.models.topic.TopicWord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentAnnotationWikiPageViewModel extends AnnotationWikiPageViewModel{

    private List<UCEMetadata> uceMetadata;
    private List<Object[]> topicDistribution;
    private List<TopicWord> topicWords;
    private List<Object[]> similarDocuments;

    public List<UCEMetadata> getUceMetadata() {
        return uceMetadata;
    }

    public void setUceMetadata(List<UCEMetadata> uceMetadata) {
        this.uceMetadata = uceMetadata;
    }

    public List<Object[]> getTopicDistribution() {
        return topicDistribution;
    }

    public void setTopicDistribution(List<Object[]> topicDistribution) {
        this.topicDistribution = topicDistribution;
    }

    public List<TopicWord> getTopicWords() {
        return topicWords;
    }

    public void setTopicWords(List<TopicWord> topicWords) {
        this.topicWords = topicWords;
    }

    public List<Object[]> getSimilarDocuments() {
        return similarDocuments;
    }

    public void setSimilarDocuments(List<Object[]> similarDocuments) {
        this.similarDocuments = similarDocuments;
    }
}
