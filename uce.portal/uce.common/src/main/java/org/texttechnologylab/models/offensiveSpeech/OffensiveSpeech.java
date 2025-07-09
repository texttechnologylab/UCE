package org.texttechnologylab.models.offensiveSpeech;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.texttechnologylab.annotations.Typesystem;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;
import org.texttechnologylab.models.corpus.Document;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "offensive-speech")
@Typesystem(types = {org.texttechnologylab.annotation.OffensiveSpeech.class})
public class OffensiveSpeech extends UIMAAnnotation implements WikiModel {

    @Getter
    @Setter
    @Column(name = "offensive", nullable = false)
    private boolean offensive;

    @Getter
    @Setter
    @Column(name = "non_offensive", nullable = false)
    private boolean nonOffensive;

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
