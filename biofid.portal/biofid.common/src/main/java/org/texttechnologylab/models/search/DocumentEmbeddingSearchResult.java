package org.texttechnologylab.models.search;

import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.rag.DocumentEmbedding;

public class DocumentEmbeddingSearchResult {

    private DocumentEmbedding documentEmbedding;
    private Document document;

    public DocumentEmbeddingSearchResult(){}

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public DocumentEmbedding getDocumentEmbedding() {
        return documentEmbedding;
    }

    public void setDocumentEmbedding(DocumentEmbedding documentEmbedding) {
        this.documentEmbedding = documentEmbedding;
    }
}
