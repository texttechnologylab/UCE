package org.texttechnologylab.uce.common.models.search;

import java.util.ArrayList;
import java.util.HashMap;

public class DocumentSearchResult {

    private int documentCount;
    private ArrayList<Integer> documentIds;
    private ArrayList<Integer> documentHits;
    private HashMap<Integer, ArrayList<PageSnippet>> searchSnippets;
    private HashMap<Long, ArrayList<PageSnippet>> searchSnippetsDocIdToSnippet;
    private HashMap<Integer, Float> searchRanks;

    private ArrayList<AnnotationSearchResult> foundNamedEntities = new ArrayList<>();
    private ArrayList<AnnotationSearchResult> foundTimes = new ArrayList<>();
    private ArrayList<AnnotationSearchResult> foundTaxons = new ArrayList<>();

    private ArrayList<AnnotationSearchResult> foundScopes = new ArrayList<>();
    private ArrayList<AnnotationSearchResult> foundXscopes = new ArrayList<>();
    private ArrayList<AnnotationSearchResult> foundEvents = new ArrayList<>();
    private ArrayList<AnnotationSearchResult> foundFoci = new ArrayList<>();
    private ArrayList<AnnotationSearchResult> foundCues = new ArrayList<>();


    public DocumentSearchResult(int documentCount,
                                ArrayList<Integer> documentIds) {
        this.documentCount = documentCount;
        this.documentIds = documentIds;
    }

    public HashMap<Long, ArrayList<PageSnippet>> getSearchSnippetsDocIdToSnippet() {
        return searchSnippetsDocIdToSnippet;
    }

    public void setSearchSnippetsDocIdToSnippet(HashMap<Long, ArrayList<PageSnippet>> searchSnippetsDocIdToSnippet) {
        this.searchSnippetsDocIdToSnippet = searchSnippetsDocIdToSnippet;
    }

    public ArrayList<AnnotationSearchResult> getFoundScopes() {
        return foundScopes;
    }

    public void setFoundScopes(ArrayList<AnnotationSearchResult> foundScopes) {
        this.foundScopes = foundScopes;
    }

    public ArrayList<AnnotationSearchResult> getFoundXscopes() {
        return foundXscopes;
    }

    public void setFoundXscopes(ArrayList<AnnotationSearchResult> foundXscopes) {
        this.foundXscopes = foundXscopes;
    }

    public ArrayList<AnnotationSearchResult> getFoundEvents() {
        return foundEvents;
    }

    public void setFoundEvents(ArrayList<AnnotationSearchResult> foundEvents) {
        this.foundEvents = foundEvents;
    }

    public ArrayList<AnnotationSearchResult> getFoundFoci() {
        return foundFoci;
    }

    public void setFoundFoci(ArrayList<AnnotationSearchResult> foundFoci) {
        this.foundFoci = foundFoci;
    }

    public ArrayList<AnnotationSearchResult> getFoundCues() {
        return foundCues;
    }

    public void setFoundCues(ArrayList<AnnotationSearchResult> foundCues) {
        this.foundCues = foundCues;
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
