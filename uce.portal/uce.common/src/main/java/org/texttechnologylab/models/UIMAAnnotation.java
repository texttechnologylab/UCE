package org.texttechnologylab.models;

import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.utils.StringUtils;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.*;

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

        // We build start and end of the annotations and store them in the TreeMap
        Map<Integer, List<UIMAAnnotation>> startTags = new TreeMap<>();
        Map<Integer, List<String>> endTags = new TreeMap<>();

        for (var annotation : annotations) {
            if (annotation.getBegin() < getBegin() || annotation.getEnd() > getEnd() || annotation.getBegin() == annotation.getEnd()) {
                continue;
            }
            var start = annotation.getBegin() - offset;
            var end = annotation.getEnd() - offset;

            //var openingTag = generateHTMLTag(annotation);
            var closingTag = "</span>";

            // Add to respective maps
            startTags.computeIfAbsent(start, k -> new ArrayList<>()).add(annotation);
            endTags.computeIfAbsent(end, k -> new ArrayList<>()).add(closingTag);
        }

        // Build the final HTML string in a single pass
        var finalText = new StringBuilder();

        for (int i = 0; i < coveredText.length(); i++) {
            // Add opening tags at this index
            if (startTags.containsKey(i)) {
                finalText.append(generateMultiHTMLTag(startTags.get(i)));
            }

            // Append the current character
            finalText.append(coveredText.charAt(i));

            // Add closing tags at this index. Add the END tags before OPENING NEW ones
            if (endTags.containsKey(i)) {
                //finalText.append(endTags.get(i).getFirst());
                for (var tag : endTags.get(i)) {
                    finalText.append(tag);
                }
            }
        }

        // We apply some heuristic post-processing to make the text more readable.
        return StringUtils.AddLineBreaks(StringUtils.CleanText(finalText.toString()), finalText.length());
    }

    private String generateMultiHTMLTag(List<UIMAAnnotation> annotations) {
        var size = annotations.size();

        if (size == 0) return "";
        else if (size == 1) return generateHTMLTag(annotations.getFirst(), true);
        else {
            var btnsHtml = new StringBuilder();
            for (var anno : annotations) {
                btnsHtml.append(generateHTMLTag(anno, true)).append(anno.getClass().getSimpleName()).append("</span>");
            }

            var tag = String.format("<span class='multi-annotation' title='%1$s'>" +
                            "<div class='multi-annotation-popup'>" +
                            btnsHtml.toString().replace("%", "%%") +
                            "</div>", UUID.randomUUID());
            return tag;
        }
    }

    // Utility method to generate an HTML opening tag for an annotation
    private String generateHTMLTag(UIMAAnnotation annotation, boolean includeTitle) {
        if (annotation instanceof NamedEntity ne) {
            return String.format(
                    "<span class='open-wiki-page annotation custom-context-menu ne-%1$s' title='%2$s' data-wid='%3$s' data-wcovered='%4$s'>",
                    ne.getType(), includeTitle ? ne.getCoveredText() : "", ne.getWikiId(), ne.getCoveredText());
        } else if (annotation instanceof Time time) {
            return String.format(
                    "<span class='open-wiki-page annotation custom-context-menu time' title='%1$s' data-wid='%2$s' data-wcovered='%3$s'>",
                    includeTitle ? time.getCoveredText() : "", time.getWikiId(), time.getCoveredText());
        } else if (annotation instanceof WikipediaLink wikipediaLink) {
            return String.format(
                    "<span class='open-wiki-page annotation custom-context-menu wiki' title='%1$s'>",
                    includeTitle ? wikipediaLink.getCoveredText() : "");
        } else if (annotation instanceof Taxon taxon) {
            return String.format(
                    "<span class='open-wiki-page annotation custom-context-menu taxon' title='%1$s' data-wid='%2$s' data-wcovered='%3$s'>",
                    includeTitle ? taxon.getCoveredText() : "", taxon.getWikiId(), taxon.getCoveredText());
        } else if (annotation instanceof Lemma lemma) {
            return String.format(
                    "<span class='open-wiki-page annotation custom-context-menu lemma' title='%1$s' data-wid='%2$s' data-wcovered='%3$s'>",
                    includeTitle ? lemma.getCoveredText() : "", lemma.getWikiId(), lemma.getCoveredText());
        }
        return "";
    }
}
