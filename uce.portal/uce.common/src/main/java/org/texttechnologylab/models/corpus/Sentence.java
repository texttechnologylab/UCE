package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="sentence")
public class Sentence extends UIMAAnnotation {

    public Sentence(){
        super(-1, -1);
    }
    public Sentence(int begin, int end) {
        super(begin, end);
    }
    public Sentence(int begin, int end, String coveredText){
        super(begin, end);
        setCoveredText(coveredText);
    }

}
