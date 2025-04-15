package org.texttechnologylab.models.viewModels.wiki;

import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.topic.TopicValueBase;
import org.texttechnologylab.models.topic.UnifiedTopic;

import java.util.List;

public class TopicValueBaseWikiPageViewModel extends AnnotationWikiPageViewModel {

    private TopicValueBase topicValueBase;
    private List<UnifiedTopic> matchingUnifiedTopics;

    public TopicValueBase getTopic() {
        return topicValueBase;
    }

    public void setTopic(TopicValueBase topic) {
        this.topicValueBase = topic;
    }

    public List<UnifiedTopic> getMatchingUnifiedTopics() {
            if (topicValueBase != null) {
                Document document = this.getDocument();

                if (document != null) {
                    String topicValue = topicValueBase.getValue();

                    List<UnifiedTopic> unifiedTopics = document.getUnifiedTopics();

                    matchingUnifiedTopics = unifiedTopics.stream()
                            .filter(unifiedTopic -> unifiedTopic.getTopics().stream()
                                    .anyMatch(topic -> topic.getValue().equals(topicValue)))
                            .toList();
                    return matchingUnifiedTopics;
                }
        }
        return null;
    }



}
