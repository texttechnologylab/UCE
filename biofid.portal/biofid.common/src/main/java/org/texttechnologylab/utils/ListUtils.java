package org.texttechnologylab.utils;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {

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
