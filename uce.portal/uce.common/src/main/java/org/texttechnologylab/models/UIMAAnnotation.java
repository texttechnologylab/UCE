package org.texttechnologylab.models;

import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.utils.StringUtils;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@MappedSuperclass
public class UIMAAnnotation extends ModelBase {

    @Column(name = "\"beginn\"")
    private int begin;
    @Column(name = "\"endd\"")
    private int end;
    @Column(columnDefinition = "TEXT")
    private String coveredText;

    public String getCoveredText() {
        return coveredText;
    }

    public String getCoveredText(String fullDocumentText) {
        var length = fullDocumentText.length();
        return fullDocumentText.substring(getBegin(), Math.min(getEnd(), length));
    }

    public void setCoveredText(String coveredText) {
        this.coveredText = coveredText.replaceAll("<", "");
    }

    public UIMAAnnotation() {
    }

    public UIMAAnnotation(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }

    /**
     * Given a list of annotations and a coveredtext, it builds a html view with the annotations being highlighted.
     */
    public String buildHTMLString(List<UIMAAnnotation> annotations, String coveredText) {
        int offset = getBegin();
        coveredText = getCoveredText(coveredText);

        // Prepare annotations grouped by start and end indices
        Map<Integer, List<String>> startTags = new TreeMap<>();
        Map<Integer, List<String>> endTags = new TreeMap<>();

        for (var annotation : annotations) {
            if (annotation.getBegin() < getBegin() || annotation.getEnd() > getEnd() || annotation.getBegin() == annotation.getEnd()) {
                continue;
            }
            int start = annotation.getBegin() - offset;
            int end = annotation.getEnd() - offset;

            // Generate HTML opening and closing tags
            String openingTag = generateHTMLTag(annotation);
            String closingTag = "</span>";

            // Add to respective maps
            startTags.computeIfAbsent(start, k -> new ArrayList<>()).add(openingTag);
            endTags.computeIfAbsent(end, k -> new ArrayList<>()).add(closingTag);
        }

        // Build the final HTML string in a single pass
        StringBuilder finalText = new StringBuilder();
        for (int i = 0; i < coveredText.length(); i++) {
            // Add opening tags at this index
            if (startTags.containsKey(i)) {
                finalText.append(startTags.get(i).getFirst());
            }

            // Append the current character
            finalText.append(coveredText.charAt(i));

            // Add closing tags at this index
            if (endTags.containsKey(i)) {
                finalText.append(endTags.get(i).getFirst());
            }
        }

        // We apply some heuristic post-processing to make the text more readable.
        return StringUtils.AddLineBreaks(StringUtils.CleanText(finalText.toString()), finalText.length());
    }

    // Utility method to generate an HTML opening tag for an annotation
    private String generateHTMLTag(UIMAAnnotation annotation) {
        if (annotation instanceof NamedEntity ne) {
            return String.format(
                    "<span class='open-wiki-page annotation custom-context-menu ne-%1$s' title='%2$s' data-wid='%3$s' data-wcovered='%4$s'>",
                    ne.getType(), ne.getCoveredText(), ne.getWikiId(), ne.getCoveredText());
        } else if (annotation instanceof Time time) {
            return String.format(
                    "<span class='open-wiki-page annotation custom-context-menu time' title='%1$s' data-wid='%2$s' data-wcovered='%3$s'>",
                    time.getCoveredText(), time.getWikiId(), time.getCoveredText());
        } else if (annotation instanceof WikipediaLink wikipediaLink) {
            return String.format(
                    "<span class='open-wiki-page annotation custom-context-menu wiki' title='%1$s'>",
                    wikipediaLink.getCoveredText());
        } else if (annotation instanceof Taxon taxon) {
            return String.format(
                    "<span class='open-wiki-page annotation custom-context-menu taxon' title='%1$s' data-wid='%2$s' data-wcovered='%3$s'>",
                    taxon.getCoveredText(), taxon.getWikiId(), taxon.getCoveredText());
        } else if (annotation instanceof Lemma lemma) {
            return String.format(
                    "<span class='open-wiki-page annotation custom-context-menu lemma' title='%1$s' data-wid='%2$s' data-wcovered='%3$s'>",
                    lemma.getCoveredText(), lemma.getWikiId(), lemma.getCoveredText());
        }
        return "";
    }
}
