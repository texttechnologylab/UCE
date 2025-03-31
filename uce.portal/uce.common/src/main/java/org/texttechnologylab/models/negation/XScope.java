package org.texttechnologylab.models.negation;

import org.texttechnologylab.models.UIMAAnnotation;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="xscope")
public class XScope extends UIMAAnnotation {
    @ManyToOne
    private CompleteNegation negation;

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
