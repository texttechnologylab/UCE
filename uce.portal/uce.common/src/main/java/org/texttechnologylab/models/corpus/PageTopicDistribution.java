package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.ModelBase;
import org.texttechnologylab.models.UIMAAnnotation;

import javax.persistence.*;

@Entity
@Table(name="pagetopicdistribution")
public class PageTopicDistribution extends TopicDistribution {
    @OneToOne()
    @JoinColumn(name="page_id")
    private Page page;
    @Column(name = "\"beginn\"")
    private Integer begin;
    @Column(name = "\"endd\"")
    private Integer end;

    public Integer getBegin() {
        return begin;
    }

    public void setBegin(Integer begin) {
        this.begin = begin;
    }

    public Integer getEnd() {
        return end;
    }

    public void setEnd(Integer end) {
        this.end = end;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }
}
