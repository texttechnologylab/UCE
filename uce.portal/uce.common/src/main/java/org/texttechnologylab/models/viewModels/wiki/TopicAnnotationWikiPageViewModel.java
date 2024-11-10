package org.texttechnologylab.models.viewModels.wiki;

import org.texttechnologylab.models.corpus.TopicDistribution;

import java.util.List;

public class TopicAnnotationWikiPageViewModel extends AnnotationWikiPageViewModel {
    private TopicDistribution topicDistribution;
    private List<? extends TopicDistribution> similarTopicDistributions;
    public TopicAnnotationWikiPageViewModel(){super();}

    public List<? extends TopicDistribution> getSimilarTopicDistributions() {
        return similarTopicDistributions;
    }

    public void setSimilarTopicDistributions(List<? extends TopicDistribution> similarTopicDistributions) {
        this.similarTopicDistributions = similarTopicDistributions;
    }

    public TopicDistribution getTopicDistribution() {
        return topicDistribution;
    }

    public void setTopicDistribution(TopicDistribution topicDistribution) {
        this.topicDistribution = topicDistribution;
    }
}
