package org.texttechnologylab.uce.common.models.corpus;

import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;
import org.texttechnologylab.uce.common.models.WikiModel;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sentence")
@Typesystem(types = {de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence.class})
public class Sentence extends UIMAAnnotation implements WikiModel {

    public Sentence() {
        super(-1, -1);
    }

    public Sentence(int begin, int end) {
        super(begin, end);
    }

    public Sentence(int begin, int end, String coveredText) {
        super(begin, end);
        setCoveredText(coveredText);
    }

    @Override
    public String getWikiId() {
        return "SENT-" + this.getId();
    }
}
