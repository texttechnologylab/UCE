package org.texttechnologylab.uce.common.models.corpus;

import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "paragraph")
@Typesystem(types = {org.texttechnologylab.annotation.ocr.abbyy.Paragraph.class})
public class Paragraph extends UIMAAnnotation {

    private int leftIndent;
    private int rightIndent;
    private int startIndent;
    private String align;
    private int lineSpacing;

    // Additional properties for paragraph styling
    // This is loaded from AnnotationComments during the import process
    private String cssClass;
    private String header;

    public Paragraph() {
        super(-1, -1);
    }

    public Paragraph(int begin, int end) {
        super(begin, end);
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public int getLeftIndent() {
        return leftIndent;
    }

    public void setLeftIndent(int leftIndent) {
        this.leftIndent = leftIndent;
    }

    public int getRightIndent() {
        return rightIndent;
    }

    public void setRightIndent(int rightIndent) {
        this.rightIndent = rightIndent;
    }

    public int getStartIndent() {
        return startIndent;
    }

    public void setStartIndent(int startIndent) {
        this.startIndent = startIndent;
    }

    public String getAlign() {
        return align;
    }

    public void setAlign(String align) {
        this.align = align;
    }

    public int getLineSpacing() {
        return lineSpacing;
    }

    public void setLineSpacing(int lineSpacing) {
        this.lineSpacing = lineSpacing;
    }

    public String getFontWeight() {
        if (getAlign() == null) return "inherit";
        return Objects.equals(getAlign().toLowerCase(), "center") ? "bold" : "inherit";
    }

    public String getUnderlined() {
        if (getAlign() == null) return "inherit";
        return Objects.equals(getAlign().toLowerCase(), "center") ? "underline" : "inherit";
    }
}
