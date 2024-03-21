package org.texttechnologylab.models.rag;

import org.texttechnologylab.models.UIMAAnnotation;

public class DocumentEmbedding extends UIMAAnnotation {

    private float[] embedding;
    private long document_id;

    public DocumentEmbedding(int begin, int end) {
        super(begin, end);
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public long getDocument_id() {
        return document_id;
    }

    public void setDocument_id(long document_id) {
        this.document_id = document_id;
    }
}
