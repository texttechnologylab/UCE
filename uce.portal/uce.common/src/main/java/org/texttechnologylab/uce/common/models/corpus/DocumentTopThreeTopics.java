package org.texttechnologylab.uce.common.models.corpus;

import org.texttechnologylab.uce.common.models.ModelBase;
import org.texttechnologylab.uce.common.models.WikiModel;

import javax.persistence.*;

@Entity
@Table(name="documenttopthreetopics")
public class DocumentTopThreeTopics extends ModelBase implements WikiModel {

    @OneToOne
    @JoinColumn(name="document_id",  insertable = false, updatable = false)
    private Document document;

    @Column(name = "document_id")
    private Long documentId;

    // Topic fields with their scores
    @Column(columnDefinition = "TEXT")
    private String topicOne;

    @Column(columnDefinition = "TEXT")
    private String topicTwo;

    @Column(columnDefinition = "TEXT")
    private String topicThree;

    @Column
    private Double topicOneScore;

    @Column
    private Double topicTwoScore;

    @Column
    private Double topicThreeScore;

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
        if (document != null) {
            this.documentId = document.getId();
        }
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getTopicOne() {
        return topicOne;
    }

    public void setTopicOne(String topicOne) {
        this.topicOne = topicOne;
    }

    public String getTopicTwo() {
        return topicTwo;
    }

    public void setTopicTwo(String topicTwo) {
        this.topicTwo = topicTwo;
    }

    public String getTopicThree() {
        return topicThree;
    }

    public void setTopicThree(String topicThree) {
        this.topicThree = topicThree;
    }

    public Double getTopicOneScore() {
        return topicOneScore;
    }

    public void setTopicOneScore(Double topicOneScore) {
        this.topicOneScore = topicOneScore;
    }

    public Double getTopicTwoScore() {
        return topicTwoScore;
    }

    public void setTopicTwoScore(Double topicTwoScore) {
        this.topicTwoScore = topicTwoScore;
    }

    public Double getTopicThreeScore() {
        return topicThreeScore;
    }

    public void setTopicThreeScore(Double topicThreeScore) {
        this.topicThreeScore = topicThreeScore;
    }

    @Override
    public String getWikiId() {
        return "DTR" + "-" + this.getId();
    }
}
