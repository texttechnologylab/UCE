package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;

import javax.persistence.*;

@Entity
@Table(name = "time")
public class Time extends UIMAAnnotation implements WikiModel {
    @Column(name = "\"valuee\"", columnDefinition = "TEXT")
    private String value;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;
    @Column(name = "page_id", insertable = false, updatable = false)
    private Long pageId;

    public Time() {
        super(-1, -1);
    }

    public Time(int begin, int end) {
        super(begin, end);
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getWikiId() {
        return "TI" + "-" + this.getId();
    }
}
