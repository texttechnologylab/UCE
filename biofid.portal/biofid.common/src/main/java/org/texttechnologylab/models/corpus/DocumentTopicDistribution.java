package org.texttechnologylab.models.corpus;

import javax.persistence.*;

@Entity
@Table(name="documenttopicdistribution")
public class DocumentTopicDistribution extends TopicDistribution{

    @OneToOne()
    @JoinColumn(name="document_id")
    private Document document;

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

}
