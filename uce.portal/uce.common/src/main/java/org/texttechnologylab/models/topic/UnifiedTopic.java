package org.texttechnologylab.models.topic;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.corpus.Document;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "unifiedtopic")
public class UnifiedTopic extends UIMAAnnotation {
    /***
     * UnifiedTopic class can be used to represent the topics in a document. A topic can be represented by a list of
     * words or category label. Each document can have multiple topics. Therefore, a unified topic creates a
     * one-to-many relationship with TopicValueBase class.
     */



    @OneToMany(mappedBy = "unifiedTopic", cascade = CascadeType.ALL, fetch=FetchType.EAGER)
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

    public void setTopics(List<TopicValueBase> topics) {
        this.topics = topics;
    }
    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

}