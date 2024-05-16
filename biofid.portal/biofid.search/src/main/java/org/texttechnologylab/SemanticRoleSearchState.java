package org.texttechnologylab;

import org.texttechnologylab.models.search.SearchType;

import java.util.ArrayList;

public class SemanticRoleSearchState extends SearchState{

    private ArrayList<String> arg0;
    private ArrayList<String> arg1;
    private ArrayList<String> argm;
    private String verb;

    public SemanticRoleSearchState(SearchType searchType) {
        super(searchType);
    }

    public ArrayList<String> getArg0() {
        return arg0;
    }

    public void setArg0(ArrayList<String> arg0) {
        this.arg0 = arg0;
    }

    public ArrayList<String> getArg1() {
        return arg1;
    }

    public void setArg1(ArrayList<String> arg1) {
        this.arg1 = arg1;
    }

    public ArrayList<String> getArgm() {
        return argm;
    }

    public void setArgm(ArrayList<String> argm) {
        this.argm = argm;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }
}
