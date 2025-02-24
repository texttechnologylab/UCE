package org.texttechnologylab.utils;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    // List of common abbreviations to exclude from splitting
    public static final String[] ABBREVIATIONS = { "Dr.", "Mr.", "Ms.", "Prof.", "Jr.", "Sr.", "zB.", "V.", "B.", "A.", "C", "M", "etc.", "S.", "ab."};

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

    public static String CleanText(String text) {
        // Remove sequences of four or more dots
        text = text.replaceAll("(\\s*\\.\\s*){4,}", ""); // Remove any sequence of 4 or more dots (with or without spaces)

        // You can add more cleaning rules here if needed
        return text;
    }

    // Method to check if a segment ends with a common abbreviation
    public static boolean EndsWithAbbreviation(String segment) {
        for (String abbr : ABBREVIATIONS) {
            if (segment.endsWith(abbr)) {
                return true;
            }
        }
        return false;
    }

    // Method to add line breaks at sensible points with deterministic randomness
    public static String AddLineBreaks(String text, long seed) {
        Random rand = new Random(seed);

        // Split the text, but avoid breaking after abbreviations and number-ending periods
        String[] splitText = text.split("(?<!\\d)(?<=\\.|\\?|\\!)");
        StringBuilder formattedText = new StringBuilder();

        for (String segment : splitText) {
            segment = segment.trim();

            // Avoid line breaks after numbers followed by periods (e.g., "18.") or abbreviations (e.g., "Dr.")
            if (!segment.matches(".*\\d+\\.$") && !EndsWithAbbreviation(segment)) {
                formattedText.append(segment);

                // Add a line break randomly based on seeded randomness
                if (rand.nextInt(3) == 0) {  // 1 in 3 chance to add a line break
                    formattedText.append("<br/>");
                    if(rand.nextInt(3) == 1){
                        formattedText.append("<br/>");
                    }
                }
            } else {
                // Append without a break if it's an abbreviation or number-ending period
                formattedText.append(segment);
            }

            // Add a space for readability
            formattedText.append(" ");
        }

        return formattedText.toString();
    }

    public static String ReplaceSpacesInQuotes(String input) {
        // Regex pattern to match any text inside quotes and replace spaces inside
        Pattern pattern = Pattern.compile("(['\"])(.*?)\\1");
        Matcher matcher = pattern.matcher(input);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            // Replace spaces within the quotes
            String modified = matcher.group(2).replace(" ", "__");
            matcher.appendReplacement(result, matcher.group(1) + modified + matcher.group(1));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
