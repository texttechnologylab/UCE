package org.texttechnologylab.models.offensiveSpeech;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.texttechnologylab.annotations.Typesystem;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.modelInfo.NamedModel;
import org.texttechnologylab.utils.Pair;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "offensivespeech")
@Typesystem(types = {org.texttechnologylab.annotation.OffensiveSpeech.class})
@NamedModel(name = "offensivespeech")
public class OffensiveSpeech extends UIMAAnnotation implements WikiModel {

    @Getter
    @Setter
    @OneToMany(mappedBy = "offensiveSpeech", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    private List<OffensiveSpeechValue> offensiveSpeechValues;

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
        return "OS" + "-" + this.getId();
    }

    public List<Pair<String, Double>> collectOffensiveSpeechValues() {
        return this.offensiveSpeechValues.stream()
            .map(value -> new Pair<>(value.getOffensiveSpeechType().getName(), value.getValue()))
            .toList();
    }

}
