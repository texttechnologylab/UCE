package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

import java.util.List;

public class WikipediaLink extends UIMAAnnotation {

    private String target;
    private String wikiData;
    private String linkType;
    private List<WikiDataHyponym> wikiDataHyponyms;
    private String coveredText;

    public WikipediaLink(int begin, int end) {
        super(begin, end);
    }

    public String getCoveredText() {
        return coveredText;
    }

    public void setCoveredText(String coveredText) {
        this.coveredText = coveredText;
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
