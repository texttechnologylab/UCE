package org.texttechnologylab.models.offensiveSpeech;

import org.texttechnologylab.annotations.Typesystem;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;
import org.texttechnologylab.models.corpus.Document;

import javax.persistence.*;

@Entity
@Table(name = "offensive-speech")
@Typesystem(types = {org.texttechnologylab.annotation.OffensiveSpeech.class})
public class OffensiveSpeech extends UIMAAnnotation implements WikiModel {



    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    public OffensiveSpeech() {
        super(-1, -1);
    }

    public OffensiveSpeech(int begin, int end) {
        super(begin, end);
    }

    public OffensiveSpeech(int begin, int end, String coveredText) {
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
