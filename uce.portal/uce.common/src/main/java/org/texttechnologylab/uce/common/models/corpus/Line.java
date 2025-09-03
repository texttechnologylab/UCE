package org.texttechnologylab.uce.common.models.corpus;

import org.texttechnologylab.uce.common.models.UIMAAnnotation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="line")
public class Line extends UIMAAnnotation {

    private int baseline;

    private int top;
    private int bottom;

    @Column(name = "\"leftt\"")
    private int left;

    @Column(name = "\"rightt\"")
    private int right;

    public Line(){
        super(-1, -1);
    }

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
