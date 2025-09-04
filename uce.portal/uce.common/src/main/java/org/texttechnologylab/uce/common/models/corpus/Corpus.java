package org.texttechnologylab.uce.common.models.corpus;

import org.joda.time.DateTime;
import org.texttechnologylab.uce.common.models.ModelBase;
import org.texttechnologylab.uce.common.models.WikiModel;
import org.texttechnologylab.uce.common.models.viewModels.CorpusViewModel;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name="corpus")
public class Corpus extends ModelBase implements WikiModel {
    private String name;
    private String author;
    private String language;
    private DateTime created;
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name="corpusid")
    private List<Document> documents;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name="corpusid")
    private List<UCEMetadataFilter> uceMetadataFilters;

    @OneToOne(mappedBy = "corpus", cascade = CascadeType.ALL, orphanRemoval = true)
    private CorpusTsnePlot corpusTsnePlot;

    @Column(columnDefinition = "TEXT")
    private String corpusJsonConfig;

    public Corpus(){
        this.created = DateTime.now();
    }

    public List<UCEMetadataFilter> getUceMetadataFilters() {
        if(uceMetadataFilters == null) return new ArrayList<>();
        uceMetadataFilters.sort(
                Comparator
                        .comparing(UCEMetadataFilter::getValueType)
                        .thenComparing(filter -> {
                            // Try to extract a number in the beginning of the key
                            String key = filter.getKey();

                            // TODO this is a special case for Coh-Metrix, should be generalized
                            // TODO duplicated in "Document getUceMetadataWithoutJson"
                            if (key.contains(":")) {
                                String[] parts = key.split(":");
                                if (parts.length > 1) {
                                    try {
                                        int number = Integer.parseInt(parts[0].trim());
                                        return String.format("%05d", number);
                                    } catch (NumberFormatException e) {
                                        // return the original key on error
                                    }
                                }
                            }

                            return key;
                        })
        );
        return uceMetadataFilters;
    }

    public void setUceMetadataFilters(List<UCEMetadataFilter> uceMetadataFilters) {
        this.uceMetadataFilters = uceMetadataFilters;
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

    @Override
    public String getWikiId() {return "C-" + this.getId(); }
}
