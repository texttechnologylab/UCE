package org.texttechnologylab.uce.common.models.corpus;

import javax.persistence.*;

@Entity
@Table(name="documentKeywordDistribution")
public class DocumentKeywordDistribution extends KeywordDistribution {

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
