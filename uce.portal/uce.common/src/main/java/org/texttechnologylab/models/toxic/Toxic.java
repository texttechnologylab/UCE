package org.texttechnologylab.models.toxic;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.texttechnologylab.annotations.Typesystem;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.modelInfo.NamedModel;

import javax.persistence.*;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "toxic")
@Typesystem(types = {org.texttechnologylab.annotation.Toxic.class})
@NamedModel(name = "toxic")
public class Toxic extends UIMAAnnotation implements WikiModel {

    @Getter
    @Setter
    @OneToMany(mappedBy = "toxic", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<ToxicValue> toxicValues;

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

    private ToxicValue getRepresentativeToxicValue() {
        if (this.toxicValues != null && !this.toxicValues.isEmpty()) {
            return this.toxicValues.stream().max(Comparator.comparingDouble(ToxicValue::getValue)).orElse(null);
        }
        return null;
    }

    public String generateToxicMarker() {
        ToxicValue rep = getRepresentativeToxicValue();
        String repValue = rep != null ? rep.getToxicType().getName() : "";
        return String.format("<span class='open-wiki-page annotation custom-context-menu toxic-marker' title='%1$s' data-wid='%2$s' data-wcovered='%3$s' data-toxic-value='%4$s'>t</span>", this.getWikiId(), this.getWikiId(), this.getCoveredText(), repValue);
    }

    public String generateToxicCoveredStartSpan() {
        ToxicValue rep = getRepresentativeToxicValue();
        String repValue = rep != null ? rep.getToxicType().getName() : "";
        return String.format("<span class='toxic-covered toxic colorable-toxic' id='t-%2$s' data-wcovered='%3$s' data-toxic-value='%4$s'>", UUID.randomUUID(), this.getWikiId(), this.getCoveredText(), repValue);
    }

    @Override
    public String getWikiId() {
        return "T" + "-" + this.getId();
    }
}
