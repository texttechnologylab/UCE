package org.texttechnologylab.models.viewModels.wiki;

import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.topic.TopicValueBase;
import org.texttechnologylab.models.topic.TopicValueBaseWithScore;
import org.texttechnologylab.models.topic.TopicWord;
import org.texttechnologylab.models.topic.UnifiedTopic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TopicValueBaseWikiPageViewModel extends AnnotationWikiPageViewModel {

    private TopicValueBase topicValueBase;
    private List<TopicValueBase> matchingTopicValueBases;

    public TopicValueBase getTopic() {
        return topicValueBase;
    }

    public void setTopic(TopicValueBase topic) {
        this.topicValueBase = topic;
    }

    public List<TopicValueBase> getMatchingTopics() {
            if (topicValueBase != null) {
                Document document = this.getDocument();

                if (document != null) {
                    String topicValue = topicValueBase.getValue();

                    List<UnifiedTopic> unifiedTopics = document.getUnifiedTopics();

                    // Collect all TopicValueBase objects that match the value
                    matchingTopicValueBases = unifiedTopics.stream()
                            .flatMap(unifiedTopic -> unifiedTopic.getTopics().stream())
                            .filter(topic -> topic.getValue().equals(topicValue))
                            .toList();
                    return matchingTopicValueBases;
                }
        }
        return null;
    }

    public boolean hasScore(){
        return topicValueBase instanceof TopicValueBaseWithScore;
    }



    public String getHighlightedText(TopicValueBase topic) {
        /**
         * Generates HTML with highlighted topic words for a given TopicValueBase
         * Words are highlighted with colors based on their probability scores:
         * - Red to green gradient for words with scores (normalized between min and max)
         *   Red indicates lower scores, green indicates higher scores
         * - Gray for words without scores
         */
        if (topic == null || topic.getWords() == null || topic.getWords().isEmpty()) {
            return topic != null ? topic.getCoveredText() : "";
        }

        String coveredText = topic.getCoveredText();
        if (coveredText == null || coveredText.isEmpty()) {
            return "";
        }

        int topicBegin = topic.getBegin();

        double minProbability = Double.MAX_VALUE;
        double maxProbability = Double.MIN_VALUE;
        boolean hasScores = false;

        for (TopicWord word : topic.getWords()) {
            double probability = word.getProbability();
            if (probability > 0) {
                hasScores = true;
                minProbability = Math.min(minProbability, probability);
                maxProbability = Math.max(maxProbability, probability);
            }
        }

        Map<Integer, String> startTags = new TreeMap<>();
        Map<Integer, String> endTags = new TreeMap<>();

        for (TopicWord word : topic.getWords()) {
            int relativeBegin = word.getBegin() - topicBegin;
            int relativeEnd = word.getEnd() - topicBegin;

            if (relativeBegin >= 0 && relativeEnd <= coveredText.length()) {
                String style;
                double probability = word.getProbability();

                if (hasScores && probability > 0) {
                    double normalizedScore = (probability - minProbability) / (maxProbability - minProbability);

                    int red = (int) ((1 - normalizedScore) * 255);
                    int green = (int) (normalizedScore * 255);
                    int blue = 0;

                    style = String.format("background-color: rgba(%d, %d, %d, 0.4); color: black;", red, green, blue);
                } else {
                    style = "background-color: rgba(150, 150, 150, 0.4); color: black;";
                }

                startTags.put(relativeBegin, "<span class='highlighted-word' style='" + style + "'>");
                endTags.put(relativeEnd, "</span>");
            }
        }

        if (startTags.isEmpty()) {
            return coveredText;
        }

        StringBuilder result = new StringBuilder();
        int currentPos = 0;

        List<Integer> allPositions = new ArrayList<>();
        allPositions.addAll(startTags.keySet());
        allPositions.addAll(endTags.keySet());
        allPositions.sort(Comparator.naturalOrder());

        for (int pos : allPositions) {
            if (pos > currentPos) {
                result.append(coveredText.substring(currentPos, pos));
            }

            if (endTags.containsKey(pos)) {
                result.append(endTags.get(pos));
            }

            if (startTags.containsKey(pos)) {
                result.append(startTags.get(pos));
            }

            currentPos = pos;
        }

        if (currentPos < coveredText.length()) {
            result.append(coveredText.substring(currentPos));
        }

        return result.toString();
    }
}
