package org.texttechnologylab.uce.common.models.topic;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;
import org.texttechnologylab.uce.common.models.WikiModel;
import org.texttechnologylab.uce.common.models.corpus.Document;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

/***
 * UnifiedTopic class can be used to represent the topics in a document. A topic can be represented by a list of
 * words or category label. Each document can have multiple topics. Therefore, a unified topic creates a
 * one-to-many relationship with TopicValueBase class.
 */
@Entity
@Table(name = "unifiedtopic")
@Typesystem(types = {org.texttechnologylab.annotation.UnifiedTopic.class})
public class UnifiedTopic extends UIMAAnnotation implements WikiModel {

    @OneToMany(mappedBy = "unifiedTopic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<TopicValueBase> topics;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    public UnifiedTopic() {
        super(-1, -1);
    }

    public UnifiedTopic(int begin, int end) {
        super(begin, end);
    }

    public UnifiedTopic(int begin, int end, String coveredText) {
        super(begin, end);
        setCoveredText(coveredText);
    }

    public List<TopicValueBase> getTopics() {
        return topics;
    }

    public List<TopicValueBase> getOrderedTopics(String order) {
        if (topics != null) {
            topics.sort((t1, t2) -> {
                double score1 = (t1 instanceof TopicValueBaseWithScore) ? ((TopicValueBaseWithScore) t1).getScore() : 0;
                double score2 = (t2 instanceof TopicValueBaseWithScore) ? ((TopicValueBaseWithScore) t2).getScore() : 0;
                return order.equals("asc") ? Double.compare(score1, score2) : Double.compare(score2, score1);
            });
        }
        return topics;
    }

    public void setTopics(List<TopicValueBase> topics) {
        this.topics = topics;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public TopicValueBase getRepresentativeTopic() {
        if (topics == null || topics.isEmpty()) {
            return null;
        }

        // Get topics ordered by score in descending order and return the first one
        List<TopicValueBase> orderedTopics = getOrderedTopics("desc");
        return orderedTopics.get(0);
    }

    public String generateTopicMarker() {
        String repTopicValue = "";
        if (this.getTopics() != null && !this.getTopics().isEmpty()) {
            var repTopic = this.getRepresentativeTopic();
            if (repTopic != null) {
                repTopicValue = repTopic.getValue();
            }
        }
        return String.format(
                "<span class='open-wiki-page annotation custom-context-menu annotation-marker topic-marker' title='%1$s' data-wid='%2$s' data-wcovered='%3$s' data-topic-value='%4$s'>t</span>",
                this.getWikiId(), this.getWikiId(), this.getCoveredText(), repTopicValue);
    }

    public String generateTopicCoveredStartSpan() {
        String repTopicValue = "";
        if (this.getTopics() != null && !this.getTopics().isEmpty()) {
            var repTopic = this.getRepresentativeTopic();
            if (repTopic != null) {
                repTopicValue = repTopic.getValue();
            }
        }
        return String.format(
                "<span class='unifiedtopic-covered topic colorable-topic' id='utopic-%2$s' data-wcovered='%3$s' data-topic-value='%4$s'>",
                UUID.randomUUID(), this.getWikiId(), this.getCoveredText(), repTopicValue);
    }

    @Override
    public String getWikiId() {
        return "UT" + "-" + this.getId();
    }
}