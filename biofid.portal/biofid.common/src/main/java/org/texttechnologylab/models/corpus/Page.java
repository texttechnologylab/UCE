package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="page")
public class Page extends UIMAAnnotation {

    private int pageNumber;
    private String pageId;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name="page_Id")
    private List<Paragraph> paragraphs;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name="page_Id")
    private List<Block> blocks;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name="page_Id")
    private List<Line> lines;

    public Page(int begin, int end, int pageNumber, String pageId){
        super(begin, end);
        this.pageNumber = pageNumber;
        this.pageId = pageId;
    }
    public Page(){
        super(-1, -1);
    }

    public List<Line> getLines() {
        return lines;
    }
    public void setLines(List<Line> lines) {
        this.lines = lines;
    }

    public List<Paragraph> getParagraphs() {
        return paragraphs;
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

    public String getPageId() {
        return pageId;
    }

    public int getPageNumber() {
        return pageNumber;
    }
}
