package org.texttechnologylab.models.search;

import org.javatuples.Tuple;

import java.util.ArrayList;
import java.util.List;

public class DocumentSearchResult {

    private int documentCount;
    private ArrayList<Integer> documentIds;
    private ArrayList<AnnotationSearchResult> foundNamedEntities;
    private ArrayList<AnnotationSearchResult> foundTimes;
    private ArrayList<AnnotationSearchResult> foundTaxons;

    public DocumentSearchResult(int documentCount,
                                ArrayList<Integer> documentIds) {
        this.documentCount = documentCount;
        this.documentIds = documentIds;
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
