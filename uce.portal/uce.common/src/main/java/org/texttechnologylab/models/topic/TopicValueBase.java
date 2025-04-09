package org.texttechnologylab.models.topic;

import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.corpus.Document;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "topic_value_base")
public class TopicValueBase extends UIMAAnnotation {

    @ManyToOne
    @JoinColumn(name = "unified_topic_id")
    private UnifiedTopic unifiedTopic;

    @Column(name = "value", nullable = false)
    private String value;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TopicWord> words;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;




    public TopicValueBase() {
        super(-1, -1);
    }

    public TopicValueBase(int begin, int end) {
        super(begin, end);
    }

    public TopicValueBase(int begin, int end, String coveredText) {
        super(begin, end);
        setCoveredText(coveredText);
    }
    public UnifiedTopic getUnifiedTopic() {
        return unifiedTopic;
    }

    public void setUnifiedTopic(UnifiedTopic unifiedTopic) {
        this.unifiedTopic = unifiedTopic;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<TopicWord> getWords() {
        return words;
    }

    public void setWords(List<TopicWord> words) {
        this.words = words;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

}