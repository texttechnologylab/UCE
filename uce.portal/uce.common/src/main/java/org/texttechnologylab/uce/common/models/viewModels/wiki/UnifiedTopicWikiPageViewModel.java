package org.texttechnologylab.uce.common.models.viewModels.wiki;

import org.texttechnologylab.uce.common.models.topic.TopicValueBase;

import java.util.List;

public class UnifiedTopicWikiPageViewModel extends AnnotationWikiPageViewModel {

    private List<TopicValueBase> topics;

    public List<TopicValueBase> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicValueBase> topics) {
        this.topics = topics;
    }

}
