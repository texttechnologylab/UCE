package org.texttechnologylab.models.negation;

import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.corpus.Document;

import javax.persistence.*;
import java.util.List;


@Entity
@Table(name="complete_negation")
public class CompleteNegation extends UIMAAnnotation {
    @Column(name = "negType", columnDefinition = "TEXT")
    private String negType;

    @OneToMany(mappedBy = "negation", cascade = CascadeType.ALL)
    private List<Focus> focusList;

    @OneToMany(mappedBy = "negation", cascade = CascadeType.ALL)
    private List<Event> eventList;

    @OneToMany(mappedBy = "negation", cascade = CascadeType.ALL)
    private List<Scope> scopeList;

    @OneToMany(mappedBy = "negation", cascade = CascadeType.ALL)
    private List<XScope> xscopeList;

    @OneToOne
    @JoinColumn(name = "cue_id")
    private Cue cue;

    @ManyToOne
    @JoinColumn(name = "document_id")
    private Document document;


    public CompleteNegation(){
        super(-1, -1);
    }
    public CompleteNegation(int begin, int end) {
        super(begin, end);
    }
    public CompleteNegation(int begin, int end, String coveredText){
        super(begin, end);
        setCoveredText(coveredText);
    }

    public String getNegType() {
        return negType;
    }

    public void setNegType(String negType) {
        this.negType = negType;
    }

    public List<Focus> getFocusList() {
        return focusList;
    }

    public void setFocusList(List<Focus> focusList) {
        this.focusList = focusList;
    }

    public List<Event> getEventList() {
        return eventList;
    }

    public void setEventList(List<Event> eventList) {
        this.eventList = eventList;
    }

    public List<Scope> getScopeList() {
        return scopeList;
    }

    public void setScopeList(List<Scope> scopeList) {
        this.scopeList = scopeList;
    }

    public List<XScope> getXscopeList() {
        return xscopeList;
    }

    public void setXscopeList(List<XScope> xscopeList) {
        this.xscopeList = xscopeList;
    }

    public Cue getCue() {
        return cue;
    }

    public void setCue(Cue cue) {
        this.cue = cue;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }
}
