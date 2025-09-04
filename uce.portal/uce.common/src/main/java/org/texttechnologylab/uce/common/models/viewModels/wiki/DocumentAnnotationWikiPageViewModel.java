package org.texttechnologylab.uce.common.models.viewModels.wiki;

import org.texttechnologylab.uce.common.models.corpus.UCEMetadata;
import org.texttechnologylab.uce.common.models.topic.TopicWord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DocumentAnnotationWikiPageViewModel extends AnnotationWikiPageViewModel{

    private List<UCEMetadata> uceMetadata;
    private List<Object[]> topicDistribution;
    private List<TopicWord> topicWords;
    private List<Object[]> similarDocuments;

    public List<UCEMetadata> getUceMetadata() {
        if(uceMetadata == null) return new ArrayList<>();
        return uceMetadata
                .stream()
                .sorted(Comparator
                        .comparing(UCEMetadata::getValueType)
                        .thenComparing(filter -> {
                            // Try to extract a number in the beginning of the key
                            String key = filter.getKey();

                            // TODO this is a special case for Coh-Metrix, should be generalized
                            // TODO duplicated in "Corpus getUceMetadataFilters"
                            if (key.contains(":")) {
                                String[] parts = key.split(":");
                                if (parts.length > 1) {
                                    try {
                                        int number = Integer.parseInt(parts[0].trim());
                                        return String.format("%05d", number);
                                    } catch (NumberFormatException e) {
                                        // return the original key on error
                                    }
                                }
                            }

                            return key;
                        })
                )
                .toList();
    }

    public void setUceMetadata(List<UCEMetadata> uceMetadata) {
        this.uceMetadata = uceMetadata;
    }

    public List<Object[]> getTopicDistribution() {
        return topicDistribution;
    }

    public void setTopicDistribution(List<Object[]> topicDistribution) {
        this.topicDistribution = topicDistribution;
    }

    public List<TopicWord> getTopicWords() {
        return topicWords;
    }

    public void setTopicWords(List<TopicWord> topicWords) {
        this.topicWords = topicWords;
    }

    public List<Object[]> getSimilarDocuments() {
        return similarDocuments;
    }

    public void setSimilarDocuments(List<Object[]> similarDocuments) {
        this.similarDocuments = similarDocuments;
    }
}
