package org.texttechnologylab.uce.common.models.corpus;

import org.texttechnologylab.uce.common.models.ModelBase;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "corpustsneplot")
public class CorpusTsnePlot extends ModelBase {

    @Column(columnDefinition = "TEXT")
    private String plotHtml;
    private Date created;

    @OneToOne()
    @JoinColumn(name="corpusid")
    private Corpus corpus;

    public Corpus getCorpus() {
        return corpus;
    }

    public void setCorpus(Corpus corpus) {
        this.corpus = corpus;
    }

    public String getPlotHtml() {
        return plotHtml;
    }

    public void setPlotHtml(String plotHtml) {
        this.plotHtml = plotHtml;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
