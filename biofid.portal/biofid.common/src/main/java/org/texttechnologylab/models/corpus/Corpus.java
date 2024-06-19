package org.texttechnologylab.models.corpus;

import org.joda.time.DateTime;
import org.texttechnologylab.models.ModelBase;
import org.texttechnologylab.models.viewModels.CorpusViewModel;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="corpus")
public class Corpus extends ModelBase {
    private String name;
    private String author;
    private String language;
    private DateTime created;
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name="corpusid")
    private List<Document> documents;

    @OneToOne(mappedBy = "corpus", cascade = CascadeType.ALL, orphanRemoval = true)
    private CorpusTsnePlot corpusTsnePlot;

    @Column(columnDefinition = "TEXT")
    private String corpusJsonConfig;

    public Corpus(){
        this.created = DateTime.now();
    }

    public CorpusViewModel getViewModel(){
        return new CorpusViewModel(this, corpusJsonConfig);
    }

    public CorpusTsnePlot getCorpusTsnePlot() {
        return corpusTsnePlot;
    }

    public void setCorpusTsnePlot(CorpusTsnePlot corpusTsnePlot) {
        this.corpusTsnePlot = corpusTsnePlot;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCorpusJsonConfig() {
        return corpusJsonConfig;
    }

    public void setCorpusJsonConfig(String corpusJsonConfig) {
        this.corpusJsonConfig = corpusJsonConfig;
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
