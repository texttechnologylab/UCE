package org.texttechnologylab.uce.common.models.corpus;

import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "page")
@Typesystem(types = {org.texttechnologylab.annotation.ocr.abbyy.Page.class}, optional = true)
public class Page extends UIMAAnnotation {
    private int pageNumber;
    private String pageIdentifier;
    private String divId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "page_Id")
    @OrderBy("begin, end")
    private List<Paragraph> paragraphs;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "page_Id")
    private List<Block> blocks;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "page_Id")
    private List<Line> lines;

    @OneToOne(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "page_id")
    private PageKeywordDistribution pageKeywordDistribution;

    public Page(int begin, int end, int pageNumber, String pageIdentifier) {
        super(begin, end);
        this.pageNumber = pageNumber;
        this.pageIdentifier = pageIdentifier;
    }

    public Page() {
        super(-1, -1);
    }

    public String getDivId() {
        return divId;
    }

    public void setDivId(String divId) {
        this.divId = divId;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public void setPageIdentifier(String pageId) {
        this.pageIdentifier = pageId;
    }

    public PageKeywordDistribution getPageKeywordDistribution() {
        return pageKeywordDistribution;
    }

    public void setPageKeywordDistribution(PageKeywordDistribution pageKeywordDistributions) {
        this.pageKeywordDistribution = pageKeywordDistributions;
    }

    public List<Line> getLines() {
        return lines;
    }

    public void setLines(List<Line> lines) {
        this.lines = lines;
    }

    public List<Paragraph> getParagraphs() {
        try {
            return paragraphs;
        } catch (Exception ex) {
            // I have no idea why, but sometimes, the lazy load of empty paragraphs throws an error.
            // There is nothing wrong with the document or page - it just throws an error here.
            // It's not a problem if the paragraphs are empty! So we just catch the error and return empty...
            // It's also not worth logging to the DB or file logger.
            System.err.println("Opened a document with unloadable lazy paragraphs.");
            return new ArrayList<>();
        }
    }

    public void setParagraphs(List<Paragraph> paragraphs) {
        this.paragraphs = paragraphs;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }

    public String getPageIdentifier() {
        return pageIdentifier;
    }

    public int getPageNumber() {
        return pageNumber;
    }
}
