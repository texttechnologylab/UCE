package org.texttechnologylab.models.negation;

import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.corpus.Page;

import javax.persistence.*;

/**
 * XScope consists of one span, ManyToOne relation to CompleteNegation (one negation can have multiple XScopes)
 */
@Entity
@Table(name="xscope")
public class XScope extends UIMAAnnotation {
    @ManyToOne
    @JoinColumn(name = "negation_id")
    private CompleteNegation negation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @Column(name = "page_id", insertable = false, updatable = false)
    private Long pageId;

    public XScope(){
        super(-1, -1);
    }
    public XScope(int begin, int end) {
        super(begin, end);
    }
    public XScope(int begin, int end, String coveredText){
        super(begin, end);
        setCoveredText(coveredText);
    }

    public CompleteNegation getNegation() {
        return negation;
    }

    public void setNegation(CompleteNegation negation) {
        this.negation = negation;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }
}
