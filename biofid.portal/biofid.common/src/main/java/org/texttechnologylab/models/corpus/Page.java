package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name="page")
public class Page extends UIMAAnnotation {

    private final int pageNumber;
    private final String pageId;
    private List<Paragraph> paragraphs;
    private List<Block> blocks;
    private List<Line> lines;

    public Page(int begin, int end, int pageNumber, String pageId){
        super(begin, end);
        this.pageNumber = pageNumber;
        this.pageId = pageId;
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
