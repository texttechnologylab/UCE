package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;

import javax.persistence.*;

@Entity
@Table(name="namedEntity")
public class NamedEntity extends UIMAAnnotation implements WikiModel {

    @Column(name = "document_id", nullable = false, insertable = true, updatable = true)
    private Long documentId;

    @Column(name = "\"typee\"")
    private String type;

    public NamedEntity(){
        super(-1, -1);
    }

    public NamedEntity(int begin, int end) {
        super(begin, end);
    }

    public Long getDocumentId(){return this.documentId;}

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getWikiId() {
        return "NE" + "-" + this.getId();
    }
}
