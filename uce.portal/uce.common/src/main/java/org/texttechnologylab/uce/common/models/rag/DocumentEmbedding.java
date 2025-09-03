package org.texttechnologylab.uce.common.models.rag;

import org.texttechnologylab.uce.common.models.ModelBase;

public class DocumentEmbedding extends ModelBase {
    private float[] embedding;
    private float[] tsne2d;
    private float[] tsne3d;
    private long document_id;

    public float[] getTsne2d() {
        return tsne2d;
    }

    public void setTsne2d(float[] tsne2d) {
        this.tsne2d = tsne2d;
    }

    public float[] getTsne3d() {
        return tsne3d;
    }

    public void setTsne3d(float[] tsne3d) {
        this.tsne3d = tsne3d;
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
