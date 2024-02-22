package org.texttechnologylab.models;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class UIMAAnnotation extends ModelBase{


    @Column(name = "\"beginn\"")
    private int begin;

    @Column(name = "\"endd\"")
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
