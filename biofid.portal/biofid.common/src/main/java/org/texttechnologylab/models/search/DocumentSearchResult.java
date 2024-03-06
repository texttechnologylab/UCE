package org.texttechnologylab.models.search;

import java.util.ArrayList;
import java.util.List;

public class DocumentSearchResult {

    private int documentCount;
    private ArrayList<Integer> documentIds;

    public DocumentSearchResult(int documentCount,
                                ArrayList<Integer> documentIds) {
        this.documentCount = documentCount;
        this.documentIds = documentIds;
    }

    public int getDocumentCount() {
        return documentCount;
    }

    public ArrayList<Integer> getDocumentIds() {
        return documentIds;
    }

}
