package org.texttechnologylab.models.negation;

import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.corpus.Document;

import javax.persistence.*;

@Entity
@Table(name="cue")
public class Cue extends UIMAAnnotation {
    @OneToOne(mappedBy = "cue") // Inverse side
    private CompleteNegation negation;

    @ManyToOne
    @JoinColumn(name = "document_id")
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
}
