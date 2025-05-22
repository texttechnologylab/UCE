package org.texttechnologylab.models.corpus;

import org.hibernate.annotations.Type;
import org.texttechnologylab.annotations.Typesystem;
import org.texttechnologylab.models.UIMAAnnotation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "paragraph")
@Typesystem(types = {org.texttechnologylab.annotation.ocr.abbyy.Paragraph.class})
public class Paragraph extends UIMAAnnotation {

    private int leftIndent;
    private int rightIndent;
    private int startIndent;
    private String align;
    private int lineSpacing;
    @Column(columnDefinition = "TEXT")
    private String coveredText;

    public Paragraph() {
        super(-1, -1);
    }

    public Paragraph(int begin, int end) {
        super(begin, end);
    }

    public String getCoveredText() {
        return coveredText;
    }

    public void setCoveredText(String coveredText) {
        this.coveredText = coveredText;
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
