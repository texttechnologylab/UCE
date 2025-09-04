package org.texttechnologylab.uce.common.models.universe;

public class CorpusUniverseNode {
    private long documentId;
    private float[] tsne2d;
    private float[] tsne3d;
    private String primaryTopic;
    private String title;
    private int documentLength;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getDocumentLength() {
        return documentLength;
    }

    public void setDocumentLength(int documentLength) {
        this.documentLength = documentLength;
    }

    public long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(long documentId) {
        this.documentId = documentId;
    }

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

    public String getPrimaryTopic() {
        return primaryTopic;
    }

    public void setPrimaryTopic(String primaryTopic) {
        this.primaryTopic = primaryTopic;
    }
}
