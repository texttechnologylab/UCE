package org.texttechnologylab.uce.common.models.viewModels.wiki;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.uce.common.models.topic.TopicWord;
import org.texttechnologylab.uce.common.models.viewModels.JsonViewModel;
import org.texttechnologylab.uce.common.utils.JsonBeautifier;

import java.util.List;
import java.util.Map;

public class CorpusWikiPageViewModel extends AnnotationWikiPageViewModel {

    @Getter
    @Setter
    private int pagesCount;
    private int documentsCount;
    private List<TopicWord> normalizedTopicWords;

    private Map<String, Double> topicDistributions;

    public Map<String, Double> getTopicDistributions() {
        return topicDistributions;
    }

    public void setTopicDistributions(Map<String, Double> topicDistributions) {
        this.topicDistributions = topicDistributions;
    }

    public List<TopicWord> getNormalizedTopicWords() {
        return normalizedTopicWords;
    }

    public void setNormalizedTopicWords(List<TopicWord> normalizedTopicWords) {
        this.normalizedTopicWords = normalizedTopicWords;
    }

    public int getDocumentsCount() {
        return documentsCount;
    }

    public void setDocumentsCount(int documentsCount) {
        this.documentsCount = documentsCount;
    }

    public List<JsonViewModel> getCorpusConfigJsonAsIterable() {
        var beautifier = new JsonBeautifier();
        return beautifier.parseJsonToViewModel(getCorpus().getCorpus().getCorpusJsonConfig());
    }


}
