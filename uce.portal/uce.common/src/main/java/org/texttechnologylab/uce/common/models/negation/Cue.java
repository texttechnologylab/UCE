package org.texttechnologylab.uce.common.models.negation;

import org.texttechnologylab.uce.common.models.UIMAAnnotation;
import org.texttechnologylab.uce.common.models.WikiModel;
import org.texttechnologylab.uce.common.models.corpus.Document;

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
