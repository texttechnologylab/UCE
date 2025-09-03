package org.texttechnologylab.uce.analysis.modules;

import org.texttechnologylab.uce.analysis.typeClasses.SentenceClass;

import java.util.HashMap;

public class Sentences {

    public HashMap<String, SentenceClass> sentences = new HashMap<>();

    public void addSentence(String begin, String end, SentenceClass sentence) {
        String id = begin + "_" + end;
        this.sentences.put(id, sentence);
    }

    public SentenceClass getSentence(String begin, String end) {
        String id = begin + "_" + end;
        return this.sentences.get(id);
    }

    public void deleteSentence(String begin, String end) {
        String id = begin + "_" + end;
        this.sentences.remove(id);
    }
}
