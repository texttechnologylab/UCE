package org.texttechnologylab.uce.common.utils;

import java.util.List;

public class EmbeddingUtils {

    /**
     * Function to perform mean pooling on a list of float arrays (embeddings)
     * @param embeddings
     * @return
     */
    public static float[] meanPooling(List<float[]> embeddings) {
        if(embeddings.isEmpty()) return null;
        if(embeddings.size() == 1) return embeddings.get(0);

        int dimensions = embeddings.get(0).length;
        int numEmbeddings = embeddings.size();

        float[] pooledEmbedding = new float[dimensions];

        // Sum up all embeddings
        for (float[] embedding : embeddings) {
            for (int i = 0; i < dimensions; i++) {
                pooledEmbedding[i] += embedding[i];
            }
        }

        // Calculate the mean by dividing the sum by the number of embeddings
        for (int i = 0; i < dimensions; i++) {
            pooledEmbedding[i] /= numEmbeddings;
        }

        return pooledEmbedding;
    }
}
