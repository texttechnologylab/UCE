package org.texttechnologylab.models.corpus;

import org.joda.time.DateTime;
import org.texttechnologylab.models.ModelBase;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="corpus")
public class Corpus extends ModelBase {
    private String name;
    private String author;
    private DateTime created;
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name="corpusid")
    private List<Document> documents;

    public Corpus(){
        this.created = DateTime.now();
    }

    public DateTime getCreated() {
        return created;
    }

    public void setCreated(DateTime created) {
        this.created = created;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }
}
