package org.texttechnologylab.models.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DocumentSearchResult {

    private int documentCount;
    private ArrayList<Integer> documentIds;
    private ArrayList<Integer> documentHits;
    private ArrayList<AnnotationSearchResult> foundNamedEntities;
    private ArrayList<AnnotationSearchResult> foundTimes;
    private ArrayList<AnnotationSearchResult> foundTaxons;
    private HashMap<Integer, ArrayList<PageSnippet>> searchSnippets;
    private HashMap<Integer, Float> searchRanks;

    public DocumentSearchResult(int documentCount,
                                ArrayList<Integer> documentIds) {
        this.documentCount = documentCount;
        this.documentIds = documentIds;
    }

    public HashMap<Integer, Float> getSearchRanks() {
        return searchRanks;
    }

    public void setSearchRanks(HashMap<Integer, Float> searchRanks) {
        this.searchRanks = searchRanks;
    }

    public HashMap<Integer, ArrayList<PageSnippet>> getSearchSnippets() {
        return searchSnippets;
    }

    public void setSearchSnippets(HashMap<Integer, ArrayList<PageSnippet>> searchSnippets) {
        this.searchSnippets = searchSnippets;
    }

    public void setDocumentCount(int documentCount) {
        this.documentCount = documentCount;
    }

    public void setDocumentIds(ArrayList<Integer> documentIds) {
        this.documentIds = documentIds;
    }

    public ArrayList<Integer> getDocumentHits() {
        return documentHits;
    }

    public void setDocumentHits(ArrayList<Integer> documentHits) {
        this.documentHits = documentHits;
    }

    public void setFoundTimes(ArrayList<AnnotationSearchResult> foundTimes) {
        this.foundTimes = foundTimes;
    }

    public void setFoundTaxons(ArrayList<AnnotationSearchResult> foundTaxons) {
        this.foundTaxons = foundTaxons;
    }

    public ArrayList<AnnotationSearchResult> getFoundTimes() {
        return foundTimes;
    }

    public ArrayList<AnnotationSearchResult> getFoundTaxons() {
        return foundTaxons;
    }

    public ArrayList<AnnotationSearchResult> getFoundNamedEntities() {
        return foundNamedEntities;
    }

    public void setFoundNamedEntities(ArrayList<AnnotationSearchResult> foundNamedEntities) {
        this.foundNamedEntities = foundNamedEntities;
    }

    public int getDocumentCount() {
        return documentCount;
    }

    public ArrayList<Integer> getDocumentIds() {
        return documentIds;
    }

}
