package org.texttechnologylab.utils;

public class StringUtils {

    /**
     * Removes special characters only at beginning and end.
     * @param str
     * @return
     */
    public static String removeSpecialCharactersAtEdges(String str) {
        return str.replaceAll("^[^a-zA-Z0-9]+", "")
                .replaceAll("[^a-zA-Z0-9]+$", "")
                .trim();
    }

    public static String lowerCaseFirstChar(String str) {
        if (str == null || str.isEmpty()) {
            return str; // Return empty string or null if input is null or empty
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}
