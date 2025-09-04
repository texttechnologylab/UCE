package org.texttechnologylab.uce.search;

import org.texttechnologylab.uce.common.models.search.SearchType;

import java.util.ArrayList;

public class SemanticRoleSearchState extends SearchState{

    private ArrayList<String> arg0 = new ArrayList<>();
    private ArrayList<String> arg1 = new ArrayList<>();
    private ArrayList<String> arg2 = new ArrayList<>();
    private ArrayList<String> argm = new ArrayList<>();
    private String verb = "";

    public SemanticRoleSearchState(SearchType searchType) {
        super(searchType);
    }

    public ArrayList<String> getArg2() {
        return arg2;
    }

    public void setArg2(ArrayList<String> arg2) {
        this.arg2 = arg2;
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
