package org.texttechnologylab.uce.common.models.viewModels.wiki;


import org.texttechnologylab.uce.common.models.negation.*;

import java.util.ArrayList;
import java.util.List;


public class NegationAnnotationWikiPageViewModel extends AnnotationWikiPageViewModel {
    private String negType;

    private List<Focus> focusList;

    private List<Event> eventList;

    private List<Scope> scopeList;

    private List<XScope> xscopeList;

    private Cue cue;

    public NegationAnnotationWikiPageViewModel(){super();}

    public String getNegType() {
        return negType;
    }

    public void setNegType(String negType) {
        this.negType = negType;
    }

    public List<Event> getEventList() {
        return eventList;
    }

    public void setEventList(List<Event> eventList) {
        this.eventList = eventList;
    }

    public List<Focus> getFocusList() {
        return focusList;
    }

    public void setFocusList(List<Focus> focusList) {
        this.focusList = focusList;
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

    public List<ArrayList<Integer>> getOffsetList() {
        List<ArrayList<Integer>> offsetList = new ArrayList<>();
        ArrayList<Integer> annoOffset = new ArrayList<>();
        annoOffset.add(cue.getBegin());
        annoOffset.add(cue.getEnd());
        offsetList.add(annoOffset);

        for (Focus anno : getFocusList()) {
            annoOffset = new ArrayList<>();
            annoOffset.add(anno.getBegin());
            annoOffset.add(anno.getEnd());
            offsetList.add(annoOffset);
        }

        for (Scope anno : getScopeList()) {
            annoOffset = new ArrayList<>();
            annoOffset.add(anno.getBegin());
            annoOffset.add(anno.getEnd());
            offsetList.add(annoOffset);
        }

        for (XScope anno : getXscopeList()) {
            annoOffset = new ArrayList<>();
            annoOffset.add(anno.getBegin());
            annoOffset.add(anno.getEnd());
            offsetList.add(annoOffset);
        }

        for (Event anno : getEventList()) {
            annoOffset = new ArrayList<>();
            annoOffset.add(anno.getBegin());
            annoOffset.add(anno.getEnd());
            offsetList.add(annoOffset);
        }
        return offsetList;
    }
}