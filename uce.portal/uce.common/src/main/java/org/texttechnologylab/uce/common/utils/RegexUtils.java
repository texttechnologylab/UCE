package org.texttechnologylab.uce.common.utils;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {

    /**
     * Given a text and a serchterm, extract the searchterms alongside a left and right context
     */
    public static List<String[]> ExtractOccurrences(String text, String searchTerm, int leftContextWords, int rightContextWords, int maxCount) {
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
            leftContext = GetLastWords(leftContext, leftContextWords);
            rightContext = GetFirstWords(rightContext, rightContextWords);

            results.add(new String[]{leftContext, searchTerm, rightContext});

            // We want to match X count instances.
            if(results.size() >= maxCount) break;
        }

        return results;
    }

    private static String GetLastWords(String text, int wordCount) {
        String[] words = text.split("\\s+");
        int start = Math.max(words.length - wordCount, 0);
        StringBuilder result = new StringBuilder();
        for (int i = start; i < words.length; i++) {
            result.append(words[i]).append(" ");
        }
        return result.toString().trim();
    }

    private static String GetFirstWords(String text, int wordCount) {
        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < Math.min(wordCount, words.length); i++) {
            result.append(words[i]).append(" ");
        }
        return result.toString().trim();
    }

    /**
     * Given a coveredtext from a Time annotation, tries to dissect it into little units.
     */
    public static TimeUnits dissectTimeAnnotationString(String timeString) {
        Pattern yearPattern = Pattern.compile("\\b(1[7-9][0-9]{2}|20[0-2][0-9])\\b");
        Pattern monthPattern = Pattern.compile("\\b(January|February|March|April|May|June|July|August|September|October|November|December|Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember)\\b", Pattern.CASE_INSENSITIVE);
        Pattern dayPattern = Pattern.compile("\\b([1-9]|[12][0-9]|3[01])\\.\\s?(January|February|March|April|May|June|July|August|September|October|November|December|Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember)?\\b", Pattern.CASE_INSENSITIVE);
        Pattern seasonPattern = Pattern.compile("\\b(Spring|Summer|Autumn|Winter|Frühling|Sommer|Herbst|Winter)\\b", Pattern.CASE_INSENSITIVE);

        Matcher yearMatcher = yearPattern.matcher(timeString);
        Matcher monthMatcher = monthPattern.matcher(timeString);
        Matcher dayMatcher = dayMatcher = dayPattern.matcher(timeString);
        Matcher seasonMatcher = seasonPattern.matcher(timeString);

        Integer year = null;
        if (yearMatcher.find()) {
            try {
                year = Integer.parseInt(yearMatcher.group());
            } catch (NumberFormatException e) {
                year = null;
            }
        }

        String month = monthMatcher.find() ? monthMatcher.group() : "";
        String day = dayMatcher.find() ? dayMatcher.group(1) : "";
        String season = seasonMatcher.find() ? seasonMatcher.group() : "";

        Date fullDate = null;

        // 1. Try full date
        fullDate = tryParse(timeString, "dd. MMMM yyyy", Locale.ENGLISH);
        if (fullDate == null)
            fullDate = tryParse(timeString, "dd. MMMM yyyy", Locale.GERMAN);

        // 2. Try month + year
        if (fullDate == null && !month.isEmpty() && year != null) {
            String fallbackString = "01. " + month + " " + year;
            fullDate = tryParse(fallbackString, "dd. MMMM yyyy", Locale.ENGLISH);
            if (fullDate == null)
                fullDate = tryParse(fallbackString, "dd. MMMM yyyy", Locale.GERMAN);
        }

        // 3. Try year only
        if (fullDate == null && year != null) {
            String fallbackString = "01. January " + year;
            fullDate = tryParse(fallbackString, "dd. MMMM yyyy", Locale.ENGLISH);
        }

        return new TimeUnits(year, month, day, fullDate, season);
    }

    private static java.sql.Date tryParse(String input, String pattern, Locale locale) {
        try {
            java.util.Date utilDate = new SimpleDateFormat(pattern, locale).parse(input);
            return new java.sql.Date(utilDate.getTime());
        } catch (ParseException e) {
            return null;
        }
    }


}
