package org.texttechnologylab;

public class DUUIInformation {
    private Sentences sentence;

    private TextClass textInformation;

    public DUUIInformation(Sentences sentence, TextClass textInformation) {
        this.sentence = sentence;
        this.textInformation = textInformation;
    }

    public Sentences getSentence() {
        return sentence;
    }

    public void setSentence(Sentences sentence) {
        this.sentence = sentence;
    }

    public TextClass getTextInformation() {
        return textInformation;
    }

    public void setTextInformation(TextClass textInformation) {
        this.textInformation = textInformation;
    }

}
