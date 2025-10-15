package org.texttechnologylab.uce.common.models.rag;

import org.texttechnologylab.uce.common.models.UIMAAnnotation;

public class DocumentChunkEmbedding extends UIMAAnnotation {

    private float[] embedding;
    private long document_id;
    private float[] tsne2D;
    private float[] tsne3D;

    public float[] getTsne2D() {
        return tsne2D;
    }

    public void setTsne2D(float[] tsne2D) {
        this.tsne2D = tsne2D;
    }

    public float[] getTsne3D() {
        return tsne3D;
    }

    public void setTsne3D(float[] tsne3D) {
        this.tsne3D = tsne3D;
    }

    public DocumentChunkEmbedding(int begin, int end) {
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
