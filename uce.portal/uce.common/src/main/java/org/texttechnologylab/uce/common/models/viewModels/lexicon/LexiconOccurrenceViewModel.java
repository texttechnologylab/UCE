package org.texttechnologylab.uce.common.models.viewModels.lexicon;

import org.texttechnologylab.uce.common.models.UIMAAnnotation;
import org.texttechnologylab.uce.common.models.corpus.Page;
import org.texttechnologylab.uce.common.utils.StringUtils;

public class LexiconOccurrenceViewModel {

    private final UIMAAnnotation uimaAnnotation;
    private final Page page;
    private long corpusId;
    private final long documentId;
    private final String occurrenceSnippetHtml;

    public LexiconOccurrenceViewModel(UIMAAnnotation uimaAnnotation,
                                      Page page){
        this.uimaAnnotation = uimaAnnotation;
        this.page = page;
        this.documentId = uimaAnnotation.getDocumentId();
        // Calculate the snippetHTML for the UI
        if(page == null) this.occurrenceSnippetHtml = uimaAnnotation.getCoveredHtmlText();
        else this.occurrenceSnippetHtml = StringUtils.buildContextSnippet(page.getCoveredText(),
                uimaAnnotation.getBegin() - page.getBegin(), uimaAnnotation.getEnd() - page.getBegin(), 100);
    }

    public UIMAAnnotation getUimaAnnotation() {
        return uimaAnnotation;
    }

    public Page getPage() {
        return page;
    }

    public long getCorpusId() {
        return corpusId;
    }

    public long getDocumentId() {
        return documentId;
    }

    public String getOccurrenceSnippetHtml() {
        return occurrenceSnippetHtml;
    }
}
