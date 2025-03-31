package org.texttechnologylab.models.negation;

import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.corpus.Document;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="xscope")
public class XScope extends UIMAAnnotation {
    @ManyToOne
    @JoinColumn(name = "negation_id")
    private CompleteNegation negation;

    @ManyToOne
    @JoinColumn(name = "document_id")
    private Document document;

    public XScope(){
        super(-1, -1);
    }
    public XScope(int begin, int end) {
        super(begin, end);
    }
    public XScope(int begin, int end, String coveredText){
        super(begin, end);
        setCoveredText(coveredText);
    }

    public CompleteNegation getNegation() {
        return negation;
    }

    public void setNegation(CompleteNegation negation) {
        this.negation = negation;
    }
}
