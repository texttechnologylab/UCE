package org.texttechnologylab.uce.analysis.typeClasses;

public class CoherenceSentence {

    private String sentence;

    private int begin;

    private int end;

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }
    public String getSentence() {
        return sentence;
    }
    public void setBegin(int begin) {
        this.begin = begin;
    }
    public int getBegin() {
        return begin;
    }
    public void setEnd(int end) {
        this.end = end;
    }
    public int getEnd() {
        return end;
    }
}
