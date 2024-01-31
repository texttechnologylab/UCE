package org.texttechnologylab.models;

public class UIMAAnnotation extends ModelBase{

    private int begin;
    private int end;

    public UIMAAnnotation(int begin, int end){
        this.begin = begin;
        this.end = end;
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }
}
