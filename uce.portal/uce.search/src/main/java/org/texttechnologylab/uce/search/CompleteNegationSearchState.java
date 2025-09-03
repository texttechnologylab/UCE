package org.texttechnologylab.uce.search;

import org.texttechnologylab.uce.common.models.search.SearchType;

import java.util.ArrayList;

public class CompleteNegationSearchState extends SearchState{

    private ArrayList<String> cue = new ArrayList<>();
    private ArrayList<String> scope = new ArrayList<>();
    private ArrayList<String> xscope = new ArrayList<>();
    private ArrayList<String> focus = new ArrayList<>();
    private ArrayList<String> event = new ArrayList<>();

    public CompleteNegationSearchState(SearchType searchType) {
        super(searchType);
    }


    public ArrayList<String> getCue() {
        return cue;
    }

    public void setCue(ArrayList<String> cue) {
        this.cue = cue;
    }

    public ArrayList<String> getScope() {
        return scope;
    }

    public void setScope(ArrayList<String> scope) {
        this.scope = scope;
    }

    public ArrayList<String> getXscope() {
        return xscope;
    }

    public void setXscope(ArrayList<String> xscope) {
        this.xscope = xscope;
    }

    public ArrayList<String> getFocus() {
        return focus;
    }

    public void setFocus(ArrayList<String> focus) {
        this.focus = focus;
    }

    public ArrayList<String> getEvent() {
        return event;
    }

    public void setEvent(ArrayList<String> event) {
        this.event = event;
    }
}
