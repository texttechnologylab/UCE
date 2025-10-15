package org.texttechnologylab.uce.common.models.search;

import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.common.models.rag.DocumentChunkEmbedding;

public class DocumentChunkEmbeddingSearchResult {

    private DocumentChunkEmbedding documentChunkEmbedding;
    private Document document;

    public DocumentChunkEmbeddingSearchResult(){}

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public DocumentChunkEmbedding getDocumentChunkEmbedding() {
        return documentChunkEmbedding;
    }

    public void setDocumentChunkEmbedding(DocumentChunkEmbedding documentChunkEmbedding) {
        this.documentChunkEmbedding = documentChunkEmbedding;
    }
}
