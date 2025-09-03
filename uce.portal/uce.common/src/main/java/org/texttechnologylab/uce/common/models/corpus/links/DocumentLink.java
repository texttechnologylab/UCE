package org.texttechnologylab.uce.common.models.corpus.links;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * A document link connects two documents with a directed edge and sorts them together logically.
 */
@Entity
@Table(name = "documentlink")
public class DocumentLink extends Link {
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

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
