package org.texttechnologylab.uce.common.models.dto;

public class EmbeddingReduceDto {
    private float[][] tsne2D;
    private float [][] tsne3D;
    private String message;
    private int status;

    public float[][] getTsne2D() {
        return tsne2D;
    }

    public void setTsne2D(float[][] tsne2D) {
        this.tsne2D = tsne2D;
    }

    public float[][] getTsne3D() {
        return tsne3D;
    }

    public void setTsne3D(float[][] tsne3D) {
        this.tsne3D = tsne3D;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
