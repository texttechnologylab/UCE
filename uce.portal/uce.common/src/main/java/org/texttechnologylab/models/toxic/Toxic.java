package org.texttechnologylab.models.toxic;

import org.texttechnologylab.annotations.Typesystem;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;
import org.texttechnologylab.models.corpus.Document;

import javax.persistence.*;

@Entity
@Table(name = "toxic")
@Typesystem(types = {org.texttechnologylab.annotation.Toxic.class})
public class Toxic extends UIMAAnnotation implements WikiModel {
    @Column(name = "toxic", nullable = false)
    private double toxic;
    @Column(name = "non_toxic", nullable = false)
    private double nonToxic;

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

    public double getToxic() {
        return toxic;
    }
    public void setToxic(double newToxic) {
        this.toxic = newToxic;
    }

    public double getNonToxic() {
        return nonToxic;
    }

    public void setNonToxic(double nonToxic) {
        this.nonToxic = nonToxic;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    @Override
    public String getWikiId() {
        return "T" + "-" + this.getId();
    }
}
