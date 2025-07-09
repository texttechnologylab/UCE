package org.texttechnologylab.models.toxic;

import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;
import org.texttechnologylab.models.corpus.Document;

import javax.persistence.*;

@Entity
@Table(name = "toxic")
public class Toxic extends UIMAAnnotation implements WikiModel {


    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    public Toxic() {
        super(-1, -1);
    }

    public Toxic(int begin, int end) {
        super(begin, end);
    }

    public Toxic(int begin, int end, String coveredText) {
        super(begin, end);
        setCoveredText(coveredText);
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    @Override
    public String getWikiId() {
        return "UT" + "-" + this.getId();
    }

}
