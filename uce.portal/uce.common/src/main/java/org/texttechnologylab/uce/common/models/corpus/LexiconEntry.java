package org.texttechnologylab.uce.common.models.corpus;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "lexicon")
public class LexiconEntry {
    @EmbeddedId
    private LexiconEntryId id;
    @Column(name = "count")
    private int count;
    private String startCharacter;

    public LexiconEntry() {}

    public LexiconEntry(LexiconEntryId id, int count) {
        this.id = id;
        this.count = count;
    }

    public String getStartCharacter() {
        return startCharacter;
    }

    public void setStartCharacter(String startCharacter) {
        this.startCharacter = startCharacter;
    }

    public LexiconEntryId getId() {
        return id;
    }

    public void setId(LexiconEntryId id) {
        this.id = id;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
