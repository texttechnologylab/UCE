package org.texttechnologylab.uce.common.models.negation;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;
import org.texttechnologylab.uce.common.models.corpus.Document;

import javax.persistence.*;
import java.util.List;

/**
 * Class for complete-negation: One complete negation always contains a cue, but can also contain multiple:
 *  - Scopes (XScopes)
 *  - Foci
 *  - Events
 *  [optional: there can also be a negation type (syntactically, semantically, etc...)]
 */
@Entity
@Table(name="completenegation")
@Typesystem(types={org.texttechnologylab.annotation.negation.CompleteNegation.class})
public class CompleteNegation extends UIMAAnnotation {
    @Column(name = "negType", columnDefinition = "TEXT")
    private String negType;

    @OneToMany(mappedBy = "negation", cascade = CascadeType.ALL, fetch=FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<Focus> focusList;

    @OneToMany(mappedBy = "negation", cascade = CascadeType.ALL, fetch=FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<Event> eventList;

    @OneToMany(mappedBy = "negation", cascade = CascadeType.ALL, fetch=FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<Scope> scopeList;

    @OneToMany(mappedBy = "negation", cascade = CascadeType.ALL, fetch=FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<XScope> xscopeList;

    @OneToOne
    @JoinColumn(name = "cue_id", unique = true)
    private Cue cue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "document_id", nullable = false)
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
