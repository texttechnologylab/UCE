package org.texttechnologylab.uce.common.models.corpus.ocr;

import org.texttechnologylab.annotation.ocr.OCRPage;

public class OCRPageAdapterImpl implements PageAdapter {
    private final OCRPage page;

    public OCRPageAdapterImpl(OCRPage page) {
        this.page = page;
    }

    @Override public int getBegin() { return page.getBegin(); }
    @Override public int getEnd() { return page.getEnd(); }
    @Override public int getPageNumber() { return page.getPageNumber(); }
    @Override public String getPageId() { return page.getPageId(); }
    @Override public String getCoveredText() { return page.getCoveredText(); }
    @Override public Object getOriginal() { return page; }
}
