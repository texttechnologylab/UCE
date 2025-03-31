package org.texttechnologylab.models.negation;

import org.texttechnologylab.models.UIMAAnnotation;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.OneToOne;

@Entity
@Table(name="cue")
public class Cue extends UIMAAnnotation {
    @OneToOne(mappedBy = "cue") // Inverse side
    private CompleteNegation negation;

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
}
