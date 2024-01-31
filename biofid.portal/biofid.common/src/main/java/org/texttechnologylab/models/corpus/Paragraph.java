package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

public class Paragraph extends UIMAAnnotation {

    private int leftIndent;
    private int rightIndent;
    private int startIndent;
    private String align;
    private int lineSpacing;

    public Paragraph(int begin, int end){
        super(begin, end);
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
}
