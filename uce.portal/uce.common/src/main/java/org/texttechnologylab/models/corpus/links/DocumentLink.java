package org.texttechnologylab.models.corpus.links;

import org.texttechnologylab.models.corpus.Document;

import javax.persistence.*;

/**
 * A document link connects two documents with a directed edge and sorts them together logically.
 */
@Entity
@Table(name = "documentlink")
public class DocumentLink extends Link {
    private long corpusId;
    @Column(name = "\"fromm\"", columnDefinition = "TEXT")
    private String from;
    @Column(name = "\"too\"", columnDefinition = "TEXT")
    private String to;

    public DocumentLink() {
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public long getCorpusId() {
        return corpusId;
    }

    public void setCorpusId(long corpusId) {
        this.corpusId = corpusId;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
