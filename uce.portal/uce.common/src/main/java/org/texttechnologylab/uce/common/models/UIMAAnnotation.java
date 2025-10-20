package org.texttechnologylab.uce.common.models;

import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.uce.common.models.corpus.*;
import org.texttechnologylab.uce.common.models.corpus.emotion.Emotion;
import org.texttechnologylab.uce.common.models.corpus.links.AnnotationLink;
import org.texttechnologylab.uce.common.models.corpus.links.AnnotationToDocumentLink;
import org.texttechnologylab.uce.common.models.corpus.links.DocumentToAnnotationLink;
import org.texttechnologylab.uce.common.models.negation.*;
import org.texttechnologylab.uce.common.models.topic.UnifiedTopic;
import org.texttechnologylab.uce.common.utils.StringUtils;

import javax.persistence.*;
import java.util.*;

@MappedSuperclass
public class UIMAAnnotation extends ModelBase implements Linkable {

    @Override
    public List<Class<? extends ModelBase>> getCompatibleLinkTypes() {
        return List.of(DocumentToAnnotationLink.class, AnnotationToDocumentLink.class, AnnotationLink.class);
    }

    @Override
    public long getPrimaryDbIdentifier() {
        return this.getId();
    }

    @Column(name = "\"beginn\"")
    private int begin;
    @Column(name = "\"endd\"")
    private int end;
    @Column(columnDefinition = "TEXT")
    private String coveredText;
    private Boolean isLexicalized = false;
    @Column(name = "document_id", insertable = false, updatable = false)
    private Long documentId;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = true)
    private Page page;

    @Getter
    @Setter
    @Column(name = "page_id", insertable = false, updatable = false)
    private Long pageId;

    public String getCoveredText() {
        if (coveredText == null) {
            return ""; // Or return an empty string "" if that's preferred
        }
        return coveredText.replaceAll("<", "");
    }

    public String getCoveredHtmlText() {
        return coveredText.replaceAll(" ", "&nbsp;").replaceAll("\n", "<br/>");
    }

    public String getCoveredText(String fullDocumentText) {
        var length = fullDocumentText.length();
        return fullDocumentText.substring(Math.min(getBegin(), length), Math.min(getEnd(), length));
    }

    public void setCoveredText(String coveredText) {
//        this.coveredText = coveredText.replaceAll("<", "")
//                .replaceAll("\n", " ")
//                .replaceAll("\r", " ");
        this.coveredText = coveredText;
    }

    public UIMAAnnotation() {
    }

    public UIMAAnnotation(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }

    public Boolean getLexicalized() {
        return isLexicalized;
    }

    public void setLexicalized(Boolean lexicalized) {
        isLexicalized = lexicalized;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public void setEnd(int end) {
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
        int errorOffset = 0;
        coveredText = getCoveredText(coveredText);

        // We build start and end of the annotations and store them in the TreeMap
        Map<Integer, List<UIMAAnnotation>> startTags = new TreeMap<>();
        Map<Integer, List<String>> endTags = new TreeMap<>();
        // These topic, sentiment and emote annotations are special cases.
        // -> But we can easily cleanup this code guys, it's redundant. - Kevin
        Map<Integer, String> topicMarkers = new TreeMap<>();
        Map<Integer, String> topicCoverWrappersStart = new TreeMap<>();
        Map<Integer, String> topicCoverWrappersEnd = new TreeMap<>();
        Map<Integer, String> sentimentCoverWrappersStart = new TreeMap<>();
        Map<Integer, String> sentimentCoverWrappersEnd = new TreeMap<>();
        Map<Integer, String> sentimentMarkers = new TreeMap<>();
        Map<Integer, String> emotionCoverWrappersStart = new TreeMap<>();
        Map<Integer, String> emotionCoverWrappersEnd = new TreeMap<>();
        Map<Integer, String> emotionMarkers = new TreeMap<>();

        for (var annotation : annotations) {
            if (annotation.getCoveredText() == null) {
                //errorOffset += 1;
                continue;
            }
            if (annotation.getBegin() < getBegin() && annotation.getEnd() < getBegin()) {
                if (annotation.getCoveredText().isEmpty()) {
                    errorOffset += 1;
                    continue;
                }
            }

            if (annotation instanceof Image) {
                // We render the image at the starting position, no end tag needed
                var start = annotation.getBegin() - offset - errorOffset;
                startTags.computeIfAbsent(start, k -> new ArrayList<>()).add(annotation);
                continue;
            }

            // We need to handle Sentiment a bit differently, as they are sentence or paragraph based annotations.
            if (annotation.getClass() != Sentiment.class && annotation.getClass() != Emotion.class) {
                if (annotation.getBegin() < getBegin()
                    || annotation.getEnd() > getEnd()
                    || annotation.getBegin() == annotation.getEnd()) {
                    continue;
                }
            }

            // So sometimes, we have broken annotations have a supposed length of "1" but really,
            // they don't as they are empty. This screws up our begin and ends though! Hence, when we
            // meet an empty annotation, track it and substract a single value of the begins and ends!
            if (annotation.getCoveredText().isEmpty()) {
                errorOffset += 1;
                continue;
            }

            if (annotation instanceof UnifiedTopic topic) {
                var start = topic.getBegin() - offset - errorOffset;
                var end = topic.getEnd() - offset - errorOffset; // marker after last char

                topicCoverWrappersStart.put(start, topic.generateTopicCoveredStartSpan());
                topicCoverWrappersEnd.put(end, "</span>");

                topicMarkers.put(end, topic.generateTopicMarker());
                continue;
            }

            if (annotation instanceof Emotion emotion) {
                var start = emotion.getBegin() - offset - errorOffset;
                var end = emotion.getEnd() - offset - errorOffset; // marker after last char

                //emotionCoverWrappersStart.put(start, "<span class='emotion'>");
                //emotionCoverWrappersEnd.put(end, "</span>");

                emotionMarkers.put(end, emotion.generateEmotionMarker());
                continue;
            }

            if (annotation instanceof Sentiment sentiment) {
                var start = sentiment.getBegin() - offset - errorOffset;
                var end = sentiment.getEnd() - offset - errorOffset;
                var sentimentColor = sentiment.getPrimaryColor();
                var bgRgba = String.format(
                        Locale.US,
                        "rgba(%d, %d, %d, %.2f)",
                        sentimentColor.getRed(),
                        sentimentColor.getGreen(),
                        sentimentColor.getBlue(),
                        sentiment.getPrimaryValue() - 0.175 // make the highest colors less popping
                );
                var color = "white";
                if (sentiment.getPrimaryValue() < 0.6) color = "var(--dark)";

                var s = String.format(
                        "<span class='sentiment' " +
                        "style='background-color: %4$s; color:%5$s' data-type='%3$s' title='%1$s' data-wid='%2$s' data-wcovered='' " +
                        "data-trigger='hover' data-toggle='popover' data-placement='right' data-html='true' data-content='%6$s'>",
                        "Sentiment: " + sentiment.getWikiId(), sentiment.getWikiId(), sentiment.getPrimaryType().toUpperCase(), bgRgba, color, sentiment.getDescription());
                sentimentCoverWrappersStart.put(start, s);
                sentimentCoverWrappersEnd.put(end, "</span>");
                sentimentMarkers.put(end, sentiment.generateSentimentMarker());
                continue;
            }

            var start = annotation.getBegin() - offset - errorOffset;
            var end = annotation.getEnd() - offset - errorOffset;

            //var openingTag = generateHTMLTag(annotation);
            var closingTag = "</span>";

            // Add to respective maps
            startTags.computeIfAbsent(start, k -> new ArrayList<>()).add(annotation);
            endTags.computeIfAbsent(end, k -> new ArrayList<>()).add(closingTag);
        }

        // Build the final HTML string in a single pass
        var finalText = new StringBuilder();

        // If a sentiment started on the previous page, it has a key less than 0
        var firstSentimentOpt = sentimentCoverWrappersStart
                .keySet()
                .stream()
                .filter(k -> k < 0)
                .max(Integer::compareTo);

        firstSentimentOpt.ifPresent(firstSentiment -> {
            finalText.append(sentimentCoverWrappersStart.get(firstSentiment));
            sentimentCoverWrappersStart.remove(firstSentiment);
        });

        // Same with Emote
        var firstEmotionOpt = emotionCoverWrappersStart
                .keySet()
                .stream()
                .filter(k -> k < 0)
                .max(Integer::compareTo);

        firstEmotionOpt.ifPresent(firstEmotion -> {
            finalText.append(emotionCoverWrappersStart.get(firstEmotion));
            emotionCoverWrappersStart.remove(firstEmotion);
        });


        // iterate +1 because we want to process the character after the last one to add the end tags
        for (int i = 0; i < coveredText.length()+1; i++) {

            if (emotionMarkers.containsKey(i)) {
                finalText.append(emotionMarkers.get(i));
            }

            // Insert the end spans
            if (topicCoverWrappersEnd.containsKey(i)) {
                finalText.append(topicCoverWrappersEnd.get(i));
            }
            if (emotionCoverWrappersEnd.containsKey(i)) {
                finalText.append(emotionCoverWrappersEnd.get(i));
                emotionCoverWrappersEnd.remove(i);
            }
            if (sentimentCoverWrappersEnd.containsKey(i)) {
                finalText.append(sentimentCoverWrappersEnd.get(i));
                sentimentCoverWrappersEnd.remove(i);
            }
            if (endTags.containsKey(i)) {
                //finalText.append(endTags.get(i).getFirst());
                for (var tag : endTags.get(i)) {
                    finalText.append(tag);
                }
            }

            // Insert start spans
            if (topicCoverWrappersStart.containsKey(i)) {
                finalText.append(topicCoverWrappersStart.get(i));
            }
            if (emotionCoverWrappersStart.containsKey(i)) {
                finalText.append(emotionCoverWrappersStart.get(i));
                emotionCoverWrappersStart.remove(i);
            }
            if (sentimentCoverWrappersStart.containsKey(i)) {
                finalText.append(sentimentCoverWrappersStart.get(i));
                sentimentCoverWrappersStart.remove(i);
            }

            // Insert marker after character
            if (topicMarkers.containsKey(i)) {
                finalText.append(topicMarkers.get(i));
            }
            if (startTags.containsKey(i)) {
                finalText.append(generateMultiHTMLTag(startTags.get(i)));
            }

            // Append the current character
            if (i < coveredText.length()) {
                finalText.append(coveredText.charAt(i));
            }
        }

        // If the sentiment goes over the page, we need to close all non-closed tags that are left
        for (var entry : sentimentCoverWrappersEnd.entrySet()) {
            finalText.append(entry.getValue());
        }
        // If the sentiment goes over the page, we need to close all non-closed tags that are left
        for (var entry : emotionCoverWrappersEnd.entrySet()) {
            finalText.append(entry.getValue());
        }

        // We apply some heuristic post-processing to make the text more readable.
        var finalTextString = finalText.toString();
        finalTextString = StringUtils.replaceCharacterOutsideTags(finalTextString, "\n", "<br/>");
        finalTextString = StringUtils.replaceCharacterOutsideTags(finalTextString, " ", "&nbsp;");
        return finalTextString;
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

            return String.format("<span class='multi-annotation' title='%1$s'>" +
                                 "<div class='multi-annotation-popup'>" +
                                 btnsHtml.toString().replace("%", "%%") +
                                 "</div><span class='ruby-text'>", UUID.randomUUID());
        }
    }

    // Utility method to generate an HTML opening tag for an annotation
    private String generateHTMLTag(UIMAAnnotation annotation, boolean includeTitle) {
        if (annotation instanceof NamedEntity ne) {
            return String.format(
                    "<span class='open-wiki-page annotation custom-context-menu ne-%1$s' title='%2$s' data-wid='%3$s' data-wcovered='%4$s'>",
                    ne.getType(), includeTitle ? ne.getCoveredText() : "", ne.getWikiId(), ne.getCoveredText());
        } else if (annotation instanceof GeoName geoName) {
            return String.format(
                    "<span class='open-wiki-page annotation custom-context-menu geoname' title='%1$s' data-wid='%2$s' data-wcovered='%3$s'>",
                    includeTitle ? geoName.getName() : "", geoName.getWikiId(), geoName.getCoveredText());
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
        } else if (annotation instanceof Cue cue) {
            return String.format(
                    "<span class='open-wiki-page annotation custom-context-menu cue' title='%1$s' data-wid='%2$s' data-wcovered='%3$s'>",
                    includeTitle ? cue.getCoveredText() : "", cue.getWikiId(), cue.getCoveredText());
        } else if (annotation instanceof Event event) {
            return String.format(
                    "<span class='annotation custom-context-menu event' title='%1$s'>",
                    includeTitle ? event.getCoveredText() : "");
        } else if (annotation instanceof Scope scope) {
            return String.format(
                    "<span class='annotation custom-context-menu scope' title='%1$s'>",
                    includeTitle ? scope.getCoveredText() : "");
        } else if (annotation instanceof XScope xscope) {
            return String.format(
                    "<span class='annotation custom-context-menu xscope' title='%1$s'>",
                    includeTitle ? xscope.getCoveredText() : "");
        } else if (annotation instanceof Focus focus) {
            return String.format(
                    "<span class='annotation custom-context-menu focus' title='%1$s'>",
                    includeTitle ? focus.getCoveredText() : "");
        } else if (annotation instanceof Image image) {
            return "<img class='document-reader-embedded-image' width='" + image.getWidth() + "' height='" + image.getHeight() + "' src='" + image.getHTMLImgSrc() + "' /><br/>";
        } else if (annotation instanceof UnifiedTopic topic) {
            // Get the representative topic if available
            String repTopicValue = "";
            if (topic.getTopics() != null && !topic.getTopics().isEmpty()) {
                var repTopic = topic.getRepresentativeTopic();
                if (repTopic != null) {
                    repTopicValue = repTopic.getValue();
                }
            }

            // Instead of wrapping the entire text, we'll just add a marker at the end
            // The actual text will be rendered normally, and only the indicator will be clickable
            return String.format(
                    "<span class='open-wiki-page annotation custom-context-menu topic colorable-topic' title='%1$s' data-wid='%2$s' data-wcovered='%3$s' data-topic-value='%4$s'>",
                    includeTitle ? repTopicValue : "", topic.getWikiId(), topic.getCoveredText(), repTopicValue);
        }

        return "";
    }
}
