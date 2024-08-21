package org.texttechnologylab.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RegexUtils {

    /**
     * Given a text and a serchterm, extract the searchterms alongside a left and right context
     * @param text
     * @param searchTerm
     * @param leftContextWords
     * @param rightContextWords
     * @return
     */
    public static List<String[]> extractOccurrences(String text, String searchTerm, int leftContextWords, int rightContextWords) {
        List<String[]> results = new ArrayList<>();

        // Constructing the regex pattern with case insensitivity and word boundaries
        String regex = "(?:(?:\\S+\\s+){0," + leftContextWords + "})\\b" + Pattern.quote(searchTerm) + "\\b(?:(?:\\s+\\S+){0," + rightContextWords + "})";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(text);

        // Iterating through matches
        while (matcher.find()) {
            String match = matcher.group();

            // Splitting the match around the search term, preserving context
            int searchTermIndex = match.toLowerCase().indexOf(searchTerm.toLowerCase());
            String leftContext = match.substring(0, searchTermIndex).trim();
            String rightContext = match.substring(searchTermIndex + searchTerm.length()).trim();

            // Extracting the exact number of words for left and right contexts
            leftContext = getLastWords(leftContext, leftContextWords);
            rightContext = getFirstWords(rightContext, rightContextWords);

            results.add(new String[]{leftContext, searchTerm, rightContext});
        }

        return results;
    }

    private static String getLastWords(String text, int wordCount) {
        String[] words = text.split("\\s+");
        int start = Math.max(words.length - wordCount, 0);
        StringBuilder result = new StringBuilder();
        for (int i = start; i < words.length; i++) {
            result.append(words[i]).append(" ");
        }
        return result.toString().trim();
    }

    private static String getFirstWords(String text, int wordCount) {
        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < Math.min(wordCount, words.length); i++) {
            result.append(words[i]).append(" ");
        }
        return result.toString().trim();
    }


}
