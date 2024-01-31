package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

public class Line extends UIMAAnnotation {

    private int baseline;
    private int top;
    private int bottom;
    private int left;
    private int right;

    public Line(int begin, int end) {
        super(begin, end);
    }

    public int getBaseline() {
        return baseline;
    }
    public void setBaseline(int baseline) {
        this.baseline = baseline;
    }

    public int getBottom() {
        return bottom;
    }
    public void setBottom(int bottom) {
        this.bottom = bottom;
    }

    public int getLeft() {
        return left;
    }
    public void setLeft(int left) {
        this.left = left;
    }

    public int getRight() {
        return right;
    }
    public void setRight(int right) {
        this.right = right;
    }

    public int getTop() {
        return top;
    }
    public void setTop(int top) {
        this.top = top;
    }
}
