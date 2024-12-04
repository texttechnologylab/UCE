package org.texttechnologylab.models.corpus;

import javax.persistence.*;

@Entity
@Table(name="documenttopicdistribution")
public class DocumentTopicDistribution extends TopicDistribution{

    @OneToOne()
    @JoinColumn(name="document_id", insertable = false, updatable = false)
    private Document document;

    @Column(name = "document_id")
    private Long documentId;

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }
}
