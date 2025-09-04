package org.texttechnologylab.uce.common.models.negation;

import org.texttechnologylab.uce.common.models.UIMAAnnotation;
import org.texttechnologylab.uce.common.models.corpus.Document;

import javax.persistence.*;

/**
 * Focus consists of one span, ManyToOne relation to CompleteNegation (one negation can have multiple Foci)
 */
@Entity
@Table(name="focus")
public class Focus extends UIMAAnnotation {
    @ManyToOne
    @JoinColumn(name = "negation_id")
    private CompleteNegation negation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    public Focus(){
        super(-1, -1);
    }
    public Focus(int begin, int end) {
        super(begin, end);
    }
    public Focus(int begin, int end, String coveredText){
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

}
