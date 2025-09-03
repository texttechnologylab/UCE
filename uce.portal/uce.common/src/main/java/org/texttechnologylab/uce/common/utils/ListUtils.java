package org.texttechnologylab.uce.common.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {
    private static final Logger logger = LogManager.getLogger(ListUtils.class);

    public static float[] convertStringArrayToFloatArray(String[] stringArray) {
        float[] floatArray = new float[stringArray.length];
        for (int i = 0; i < stringArray.length; i++) {
            try {
                floatArray[i] = Float.parseFloat(stringArray[i]);
            } catch (NumberFormatException e) {
                logger.error("Error parsing string to float: " + stringArray[i]);
                floatArray[i] = 0.0f;
            }
        }
        return floatArray;
    }

    /**
     * Partitions a list into sublists of a specified size.
     *
     * @param list The list to partition.
     * @param chunkSize The size of each chunk.
     * @param <T> The type of elements in the list.
     * @return A list of sublists, each of size chunkSize (except possibly the last one).
     */
    public static <T> List<List<T>> partitionList(List<T> list, int chunkSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            partitions.add(list.subList(i, Math.min(i + chunkSize, list.size())));
        }
        return partitions;
    }

}
