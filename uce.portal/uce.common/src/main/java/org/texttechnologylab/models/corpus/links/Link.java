package org.texttechnologylab.models.corpus.links;

import org.texttechnologylab.models.ModelBase;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * A link is a logical connection between (annotation) objects. There are several different levels of linking:
 * - Document to Document
 * - Annotation to Annotation
 * - Text to Text
 * And mixing all those. It's like a directed graph, and the link is an edge between two nodes.
 */
@MappedSuperclass
public class Link extends ModelBase {

    @Column(columnDefinition = "TEXT")
    private String type;
    private String linkId;
    /**
     * This is the PK of the document in the db, so the 'id'.
     */
    private long fromId;
    /**
     * This is the PK of the document in the db, so the 'id'.
     */
    private long toId;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public long getFromId() {
        return fromId;
    }

    public void setFromId(long fromId) {
        this.fromId = fromId;
    }

    public long getToId() {
        return toId;
    }

    public void setToId(long toId) {
        this.toId = toId;
    }
}
