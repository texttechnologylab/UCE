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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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

    private OffensiveSpeechValue getRepresentativeOffensiveSpeechValue() {
        if (this.offensiveSpeechValues != null && !this.offensiveSpeechValues.isEmpty()) {
            return this.offensiveSpeechValues.stream().max(Comparator.comparingDouble(OffensiveSpeechValue::getValue)).orElse(null);
        }
        return null;
    }

    public String generateOffensiveSpeechMarker() {
        OffensiveSpeechValue rep = getRepresentativeOffensiveSpeechValue();
        String repValue = rep != null ? rep.getOffensiveSpeechType().getName() : "";
        return String.format("<span class='open-wiki-page annotation custom-context-menu offensive-marker' title='%1$s' data-wid='%2$s' data-wcovered='%3$s' data-offensive-value='%4$s'>os</span>", this.getWikiId(), this.getWikiId(), this.getCoveredText(), repValue);
    }

    public String generateOffensiveSpeechCoveredStartSpan() {
        OffensiveSpeechValue rep = getRepresentativeOffensiveSpeechValue();
        String repValue = rep != null ? rep.getOffensiveSpeechType().getName() : "";
        return String.format("<span class='offensive-covered offensive colorable-offensive' id='os-%2$s' data-wcovered='%3$s' data-offensive-value='%4$s'>", UUID.randomUUID(), this.getWikiId(), this.getCoveredText(), repValue);
    }

    @Override
    public String getWikiId() {
        return "OS" + "-" + this.getId();
    }

    public List<Pair<String, Double>> collectOffensiveSpeechValues() {
        return this.offensiveSpeechValues.stream().map(value -> new Pair<>(value.getOffensiveSpeechType().getName(), value.getValue())).toList();
    }

}
