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
import java.util.List;

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

    @Override
    public String getWikiId() {
        return "T" + "-" + this.getId();
    }
}
