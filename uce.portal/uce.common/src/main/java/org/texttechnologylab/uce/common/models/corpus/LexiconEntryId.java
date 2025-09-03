package org.texttechnologylab.uce.common.models.corpus;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Since the LexiconEntry has a composite primary key (coveredtext and type), we need this
 * embeddable implementation of the id.
 */
@Embeddable
public class LexiconEntryId implements Serializable {

    @Column(name = "coveredtext", columnDefinition = "TEXT")
    private String coveredText;

    @Column(name = "typee")
    private String type;

    public LexiconEntryId() {}

    public LexiconEntryId(String coveredText, String type) {
        this.coveredText = coveredText;
        this.type = type;
    }

    public String getCoveredText() {
        return coveredText;
    }

    public void setCoveredText(String coveredText) {
        this.coveredText = coveredText;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LexiconEntryId)) return false;
        LexiconEntryId that = (LexiconEntryId) o;
        return Objects.equals(coveredText, that.coveredText) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coveredText, type);
    }
}
