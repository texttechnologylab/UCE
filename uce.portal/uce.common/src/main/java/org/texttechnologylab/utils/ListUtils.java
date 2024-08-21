package org.texttechnologylab.utils;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {

    public static float[] convertStringArrayToFloatArray(String[] stringArray) {
        float[] floatArray = new float[stringArray.length];
        for (int i = 0; i < stringArray.length; i++) {
            try {
                floatArray[i] = Float.parseFloat(stringArray[i]);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing string to float: " + stringArray[i]);
                floatArray[i] = 0.0f; // or any default value you prefer
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
