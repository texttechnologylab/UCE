package org.texttechnologylab.uce.common.utils;


import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    // List of common abbreviations to exclude from splitting
    public static final String[] ABBREVIATIONS = {"Dr.", "Mr.", "Ms.", "Prof.", "Jr.", "Sr.", "zB.", "V.", "B.", "A.", "C", "M", "etc.", "S.", "ab."};
    private static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = Map.ofEntries(
            // Text / Documents
            Map.entry("txt", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("tsv", "text/tab-separated-values"),
            Map.entry("html", "text/html"),
            Map.entry("htm", "text/html"),
            Map.entry("xml", "application/xml"),
            Map.entry("xmi", "application/xmi+xml"),
            Map.entry("json", "application/json"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("rtf", "application/rtf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),

            // Images
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("webp", "image/webp"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("ico", "image/x-icon"),

            // Audio
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("m4a", "audio/mp4"),

            // Video
            Map.entry("mp4", "video/mp4"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("avi", "video/x-msvideo"),
            Map.entry("webm", "video/webm"),
            Map.entry("mkv", "video/x-matroska"),

            // Archives / Compressed
            Map.entry("zip", "application/zip"),
            Map.entry("bz2", "application/x-bzip2"),
            Map.entry("tar", "application/x-tar"),
            Map.entry("gz", "application/gzip"),
            Map.entry("rar", "application/vnd.rar"),
            Map.entry("7z", "application/x-7z-compressed"),

            // Code / Config
            Map.entry("js", "application/javascript"),
            Map.entry("jsonld", "application/ld+json"),
            Map.entry("css", "text/css"),
            Map.entry("java", "text/x-java-source"),
            Map.entry("py", "text/x-python"),
            Map.entry("c", "text/x-c"),
            Map.entry("cpp", "text/x-c++"),
            Map.entry("h", "text/x-c"),
            Map.entry("sh", "application/x-sh"),
            Map.entry("yaml", "application/x-yaml"),
            Map.entry("yml", "application/x-yaml"),
            Map.entry("md", "text/markdown"),

            // Fallback
            Map.entry("bin", "application/octet-stream")
    );
    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION;
    static {
        Map<String, String> reverseMap = new HashMap<>();
        for (Map.Entry<String, String> entry : EXTENSION_TO_CONTENT_TYPE.entrySet()) {
            reverseMap.putIfAbsent(entry.getValue(), entry.getKey());
        }
        CONTENT_TYPE_TO_EXTENSION = Collections.unmodifiableMap(reverseMap);
    }

    /**
     * Get the content type (MIME) for a given filename or extension.
     * Defaults to "application/octet-stream" for unknown types.
     */
    public static String getContentTypeByExtension(String filenameOrExtension) {
        String ext = filenameOrExtension;
        int dotIndex = filenameOrExtension.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex < filenameOrExtension.length() - 1) {
            ext = filenameOrExtension.substring(dotIndex + 1);
        }
        return EXTENSION_TO_CONTENT_TYPE.getOrDefault(ext.toLowerCase(), "application/octet-stream");
    }

    /**
     * Get the file extension for a given content type.
     * Defaults to "bin" for unknown types.
     */
    public static String getExtensionByContentType(String contentType) {
        return CONTENT_TYPE_TO_EXTENSION.getOrDefault(contentType.toLowerCase(), "bin");
    }

    public static float tryParseFloat(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }

    /**
     * Converts a GBIF species URL into a BioFID ontology URL.
     * Example:
     *   https://www.gbif.org/species/2487879
     * → https://www.biofid.de/bio-ontologies/gbif/2487879
     *
     * @param gbifUrl the GBIF species URL
     * @return the corresponding BioFID ontology URL, or null if no valid ID is found
     */
    public static String gbifToBIOfidUrl(String gbifUrl) {
        if (gbifUrl == null || gbifUrl.isBlank()) {
            return null;
        }

        // Extract the numeric ID at the end of the GBIF URL
        String[] parts = gbifUrl.trim().split("/");
        String lastPart = parts[parts.length - 1];

        // Check if it's actually a number
        if (!lastPart.matches("\\d+")) {
            return null; // not a valid GBIF species URL
        }

        return "https://www.biofid.de/bio-ontologies/gbif/" + lastPart;
    }

    /**
     * Removes special characters only at beginning and end.
     *
     * @param str
     * @return
     */
    public static String removeSpecialCharactersAtEdges(String str) {
        return str.replaceAll("^[^a-zA-Z0-9]+", "")
                .replaceAll("[^a-zA-Z0-9]+$", "")
                .trim();
    }

    public static String tryRoundToTwoDecimals(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        try {
            double number = Double.parseDouble(input);
            if (Double.isNaN(number) || Double.isInfinite(number)) {
                return input;
            }

            var df = new DecimalFormat("0.##");
            return df.format(number);
        } catch (NumberFormatException e) {
            return input;
        }
    }

    public static String lowerCaseFirstChar(String str) {
        if (str == null || str.isEmpty()) {
            return str; // Return empty string or null if input is null or empty
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    public static List<Character> getAlphabetAsList() {
        List<Character> alphabet = new ArrayList<>();
        for (char c = 'A'; c <= 'Z'; c++) {
            alphabet.add(c);
        }
        return alphabet;
    }

    public static String CleanText(String text) {
        // Remove sequences of four or more dots
        text = text.replaceAll("(\\s*\\.\\s*){4,}", ""); // Remove any sequence of 4 or more dots (with or without spaces)

        // You can add more cleaning rules here if needed
        return text;
    }

    // Method to check if a segment ends with a common abbreviation
    public static boolean endsWithAbbreviation(String segment) {
        for (String abbr : ABBREVIATIONS) {
            if (segment.endsWith(abbr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a given string has any of the listed file extensions
     */
    public static boolean checkIfFileHasExtension(String s, String[] extensions) {
        return Arrays.stream(extensions).anyMatch(s::endsWith);
    }

    /**
     * replace special characters with html variant, to persist appearance
     *
     * @param text
     * @return
     */
    public static String getHtmlText(String text) {
        return text.replaceAll("\n", "<br/>").replaceAll(" ", "&nbsp;");
    }

    /**
     * Builds a snippet from a given text, where left and right side of the begin and end are added.
     */
    public static String buildContextSnippet(String text, int begin, int end, int contextLength) {
        if (text == null || text.isEmpty() || begin < 0 || end > text.length() || begin >= end) {
            return "";
        }

        var contextBefore = Math.max(0, begin - contextLength);
        var contextAfter = Math.min(text.length(), end + contextLength);

        // Expand left context until we hit a whitespace or reach the start
        while (contextBefore > 0 && !Character.isWhitespace(text.charAt(contextBefore))) {
            contextBefore--;
        }

        // Expand right context until we hit a whitespace or reach the end
        while (contextAfter < text.length() && !Character.isWhitespace(text.charAt(contextAfter - 1))) {
            contextAfter++;
        }

        var before = text.substring(contextBefore, begin);
        var word = text.substring(begin, end);
        var after = text.substring(end, contextAfter);

        return before + "<b>" + word + "</b>" + after;
    }


    // Method to add line breaks at sensible points with deterministic randomness
    public static String addLineBreaks(String text, long seed) {
        Random rand = new Random(seed);

        // Split the text, but avoid breaking after abbreviations and number-ending periods
        String[] splitText = text.split("(?<!\\d)(?<=\\.|\\?|\\!)");
        StringBuilder formattedText = new StringBuilder();

        for (String segment : splitText) {
            segment = segment.trim();

            // Avoid line breaks after numbers followed by periods (e.g., "18.") or abbreviations (e.g., "Dr.")
            if (!segment.matches(".*\\d+\\.$") && !endsWithAbbreviation(segment)) {
                formattedText.append(segment);

                // Add a line break randomly based on seeded randomness
                if (rand.nextInt(3) == 0) {  // 1 in 3 chance to add a line break
                    formattedText.append("<br/>");
                    if (rand.nextInt(3) == 1) {
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

    public static String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf(".");
        if (lastIndex == -1 || lastIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastIndex + 1);
    }

    public static String replaceSpacesInQuotes(String input) {
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

    public static String BIOFID_URL_BASE = "https://www.biofid.de/bio-ontologies/gbif/";

    private static String protectTagContent(String html, Pattern pattern, List<String> protectedParts, Map<String, String> placeholderMap) {
        Matcher matcher = pattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        int index = 0;

        while (matcher.find()) {
            String match = matcher.group();
            String placeholder = "%%UCE_PLACEHOLDER_" + index++ + "%%";
            protectedParts.add(match);
            placeholderMap.put(placeholder, match);
            matcher.appendReplacement(sb, placeholder);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public static String replaceCharacterOutsideTags(String input, String target, String replacement) {
        // this is a replacement for "replaceCharacterOutsideSpan" that allows to specify multiple tags
        // TODO check that this is actually working and produces equivalent results

        List<String> protectedParts = new ArrayList<>();
        Map<String, String> placeholderMap = new HashMap<>();

        Pattern spanPattern = Pattern.compile("<span.*?>.*?</span>", Pattern.DOTALL);
        input = protectTagContent(input, spanPattern, protectedParts, placeholderMap);

        Pattern imgPattern = Pattern.compile("<img\\b[^>]*?>", Pattern.CASE_INSENSITIVE);
        input = protectTagContent(input, imgPattern, protectedParts, placeholderMap);

        input = input.replaceAll(Pattern.quote(target), Matcher.quoteReplacement(replacement));
        for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
            input = input.replace(entry.getKey(), entry.getValue());
        }

        return input;
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
            } else if (i <= input.length() - 7 && input.substring(i, i + 7).equals("</span>")) {
                inSpan = false;
                result.append("</span>");
                i += 7;
            } else {
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

            idx++;
        }
        return result.toString();
    }

}
