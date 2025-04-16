package org.texttechnologylab.models.topic;

import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;
import org.texttechnologylab.models.corpus.Document;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "topicvaluebase")
@Inheritance(strategy = InheritanceType.JOINED)
public class TopicValueBase extends UIMAAnnotation implements WikiModel {
    /***
     * TopicValueBase class can be used to represent a topic with a label in a document. It also allows to represent a
     * topic with a list of word. Therefore, TopicValueBase class has one-to-many relationship with TopicWord class.
     *
     */

    @ManyToOne
    @JoinColumn(name = "unifiedTopic_id")
    private UnifiedTopic unifiedTopic;

    @Column(name = "value", nullable = false)
    private String value;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
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

    @Override
    public String getWikiId() {
        return "TVB" + "-" + this.getId();
    }
}