package org.texttechnologylab;

import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.search.SearchLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A class that holds all states of a biofid search. We can use this class to serialize the search. It shouldn't hold any services.
 */
public class BiofidSearchState {
    private UUID searchId;

    /**
     * The raw search phrase
     */
    private String searchPhrase;
    private List<String> searchTokens;
    private List<SearchLayer> searchLayers;
    private Integer currentPage = 0;
    private Integer take = 15;

    /**
     * These are the current, paginated list of documents
     */
    private List<Document> currentDocuments;

    public BiofidSearchState(){
        this.searchId = UUID.randomUUID();
    }

    public void setCurrentDocuments(List<Document> currentDocuments) {
        this.currentDocuments = currentDocuments;
    }

    public List<Document> getCurrentDocuments() {
        return currentDocuments;
    }

    public UUID getSearchId() {
        return searchId;
    }

    public void setSearchId(UUID searchId) {
        this.searchId = searchId;
    }

    public String getSearchPhrase() {
        return searchPhrase;
    }

    public void setSearchPhrase(String searchPhrase) {
        this.searchPhrase = searchPhrase;
    }

    public List<String> getSearchTokens() {
        return searchTokens;
    }

    public void setSearchTokens(List<String> searchTokens) {
        this.searchTokens = searchTokens;
    }

    public List<SearchLayer> getSearchLayers() {
        return searchLayers;
    }

    public void setSearchLayers(List<SearchLayer> searchLayers) {
        this.searchLayers = searchLayers;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getTake() {
        return take;
    }

    public void setTake(Integer take) {
        this.take = take;
    }
}
