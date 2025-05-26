package org.texttechnologylab.models.negation;

import org.texttechnologylab.annotations.Typesystem;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.corpus.Page;

import javax.persistence.*;

/**
 * Class implements the Cue. Consists of one span. (OneToOne relation with CompleteNegation)
 */
@Entity
@Table(name="cue")
public class Cue extends UIMAAnnotation implements WikiModel {
    @OneToOne
    @JoinColumn(name = "negation_id", unique = true)
    private CompleteNegation negation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    public Cue(){
        super(-1, -1);
    }
    public Cue(int begin, int end) {
        super(begin, end);
    }
    public Cue(int begin, int end, String coveredText){
        super(begin, end);
        setCoveredText(coveredText);
    }

    public CompleteNegation getNegation() {
        return negation;
    }

    public void setNegation(CompleteNegation negation) {
        this.negation = negation;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    @Override
    public String getWikiId() {
        return "CU" + "-" + this.getId();
    }

}
