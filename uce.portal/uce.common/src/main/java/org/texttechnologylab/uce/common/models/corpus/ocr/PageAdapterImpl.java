package org.texttechnologylab.uce.common.models.corpus.ocr;

import org.texttechnologylab.annotation.ocr.abbyy.Page;

public class PageAdapterImpl implements PageAdapter {
    private final Page page;
    private final int pageNumber;

    public PageAdapterImpl(Page page, int pageNumber) {
        this.pageNumber = pageNumber;
        this.page = page;
    }

    @Override public int getBegin() { return page.getBegin(); }
    @Override public int getEnd() { return page.getEnd(); }
    @Override public int getPageNumber() { return this.pageNumber; }
    @Override public String getPageId() { return page.getId(); }
    @Override public String getCoveredText() { return page.getCoveredText(); }
    @Override public Object getOriginal() { return page; }
}

