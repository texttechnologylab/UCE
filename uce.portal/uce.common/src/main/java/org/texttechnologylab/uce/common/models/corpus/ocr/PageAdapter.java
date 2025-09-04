package org.texttechnologylab.uce.common.models.corpus.ocr;

public interface PageAdapter {
    int getBegin();
    int getEnd();
    int getPageNumber();
    String getPageId();
    String getCoveredText();
    Object getOriginal(); // optional, if needed to trace back to JCas type
}
