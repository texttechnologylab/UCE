package org.texttechnologylab.utils;

import java.util.ArrayList;
import java.util.List;
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

    // https://en.wikipedia.org/wiki/Taxonomic_rank#:~:text=Main%20ranks,-In%20his%20landmark&text=Today%2C%20the%20nomenclature%20is%20regulated,family%2C%20genus%2C%20and%20species.
    public static final String[] TAX_RANKS = {"G::", "F::", "O::", "C::", "P::", "K::"};

    public static String GetFullTaxonRankByCode(String code){
        return switch (code) {
            case "C" -> "class";
            case "F" -> "family";
            case "K" -> "kingdom";
            case "P" -> "phylum";
            case "O" -> "order";
            case "G" -> "genus";
            default -> null;
        };
    }

    public static String replaceCharacterOutsideSpan(String input, char targetChar, String replacement) {
        StringBuilder result = new StringBuilder();
        boolean inSpan = false;
        int i = 0;
        while (i < input.length()) {
            if (i <= input.length() - 6 && input.substring(i, i + 6).equals("<span ")) {
                inSpan = true;
                while (i < input.length() && input.charAt(i) != '>') {
                    result.append(input.charAt(i));
                    i++;
                }
                if (i < input.length()) {
                    result.append(input.charAt(i));
                    i++;
                }
            }

            else if (i <= input.length() - 7 && input.substring(i, i + 7).equals("</span>")) {
                inSpan = false;
                result.append("</span>");
                i += 7;
            }

            else {
                char currentChar = input.charAt(i);
                if (inSpan) {

                    result.append(currentChar);
                } else {

                    if (currentChar == targetChar) {
                        result.append(replacement);
                    } else {
                        result.append(currentChar);
                    }
                }
                i++;
            }
        }

        return result.toString();
    }

    public static String ConvertSparqlQuery(String query) {
        // Regex pattern to match <https://www.biofid.de/bio-ontologies/gbif/123123>
        Pattern pattern = Pattern.compile("<https://www\\.biofid\\.de/bio-ontologies/gbif/(\\d+)>");

        // Replace with bio:123123
        Matcher matcher = pattern.matcher(query);
        return matcher.replaceAll("bio:$1");
    }

    public static final String[] TIME_COMMANDS = {"Y::", "M::", "D::", "S::"};
    public static String GetFullTimeUnitByCode(String code){
        return switch (code) {
            case "Y" -> "year";
            case "M" -> "month";
            case "D" -> "day";
            case "S" -> "season";
            default -> null;
        };
    }

    public static String mergeBoldTags(String input) {
        StringBuilder result = new StringBuilder();
        boolean inBold = false;
        StringBuilder boldBuffer = new StringBuilder();

        int i = 0;
        while (i < input.length()) {
            if (input.startsWith("<b>", i)) {
                int endTag = input.indexOf("</b>", i);
                if (endTag != -1) {
                    String content = input.substring(i + 3, endTag);

                    if (!inBold) {
                        inBold = true;
                        boldBuffer.setLength(0); // Clear buffer
                    }
                    boldBuffer.append(content);
                    i = endTag + 4; // Skip past </b>
                    // Check if next tag is also <b> (i.e., adjacent bold)
                    if (!input.startsWith("<b>", i)) {
                        // No more adjacent bolds — flush
                        result.append("<b>").append(boldBuffer).append("</b>");
                        inBold = false;
                    }
                } else {
                    break; // malformed HTML
                }
            } else {
                // Just copy non-bold text (like spaces or punctuation)
                result.append(input.charAt(i));
                i++;
            }
        }

        return result.toString();
    }

    public static String addBoldTags(String input, List<ArrayList<Integer>> offsets) {
        StringBuilder result = new StringBuilder();
        int idx = 0;
        for (char c : input.toCharArray()) {
            boolean inBold = false;
            for (ArrayList<Integer> offset : offsets) {
                if (idx < offset.getLast() && offset.getFirst() <= idx) {
                    inBold = true;
                    break;
                }
            }
            if (inBold) {
                result.append("<b>").append(c).append("</b>");
            } else {
                result.append(c);
            }

            idx ++;
        }
        return result.toString();
    }

}
