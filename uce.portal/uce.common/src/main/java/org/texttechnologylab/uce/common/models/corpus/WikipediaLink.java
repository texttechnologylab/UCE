package org.texttechnologylab.uce.common.models.corpus;

import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "wikipediaLink")
@Typesystem(types = {org.hucompute.textimager.uima.type.wikipedia.WikipediaLink.class})
public class WikipediaLink extends UIMAAnnotation {

    private String target;
    private String wikiData;
    private String linkType;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "wikipediaLink_Id")
    private List<WikiDataHyponym> wikiDataHyponyms;

    public WikipediaLink() {
        super(-1, -1);
    }

    public WikipediaLink(int begin, int end) {
        super(begin, end);
    }

    public String getWikiData() {
        return wikiData;
    }

    public String getLinkType() {
        return linkType;
    }

    public String getTarget() {
        return target;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public List<WikiDataHyponym> getWikiDataHyponyms() {
        return wikiDataHyponyms;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setWikiData(String wikiData) {
        this.wikiData = wikiData;
    }

    public void setWikiDataHyponyms(List<WikiDataHyponym> wikiDataHyponyms) {
        this.wikiDataHyponyms = wikiDataHyponyms;
    }
}
