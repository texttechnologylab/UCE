package org.texttechnologylab.uce.common.models.viewModels.wiki;

import org.texttechnologylab.uce.common.models.corpus.DocumentTopThreeTopics;
import org.texttechnologylab.uce.common.models.topic.TopicWord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class TopicWikiPageViewModel extends AnnotationWikiPageViewModel{
    private DocumentTopThreeTopics documentTopThreeTopics;

    private List<TopicWord> topicTerms;
    private List<Object[]> topDocumentsForTopic;
    private List<Object[]> similarTopics;

    public DocumentTopThreeTopics getDocumentTopicDistribution() {
        return documentTopThreeTopics;
    }

    public void setDocumentTopicDistribution(DocumentTopThreeTopics documentTopThreeTopics) {
        this.documentTopThreeTopics = documentTopThreeTopics;
    }


    public List<TopicWord> getTopicTerms() {
        return this.topicTerms;
    }

    public void setTopicTerms(List<TopicWord> topicTerms) {
        this.topicTerms = topicTerms;
    }

    public List<Object[]> getTopDocumentsForTopic() {
        return topDocumentsForTopic;
    }

    public void setTopDocumentsForTopic(List<Object[]> topDocumentsForTopic) {
        this.topDocumentsForTopic = topDocumentsForTopic;
    }

    public List<Object[]> getSimilarTopics() {
        return similarTopics;
    }

    public void setSimilarTopics(List<Object[]> similarTopics) {
        this.similarTopics = similarTopics;
    }

    public List<Map<String, Object>> getWordCloudData() {
        List<Map<String, Object>> terms = new ArrayList<>();

        if (topicTerms != null && !topicTerms.isEmpty()) {
            for (TopicWord topicWord : topicTerms) {
                Map<String, Object> term = new HashMap<>();
                term.put("term", topicWord.getWord());
                term.put("weight", topicWord.getProbability());
                terms.add(term);
            }
        }

        return terms;
    }

    public List<Map<String, Object>> getDocumentDistributionData() {
        List<Map<String, Object>> documentDistData = new ArrayList<>();

        if (topDocumentsForTopic != null && !topDocumentsForTopic.isEmpty()) {
            for (Object[] docData : topDocumentsForTopic) {
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("documentId", docData[0]);
                dataPoint.put("documentTitle", docData[1]);
                dataPoint.put("weight", docData[2]);
                documentDistData.add(dataPoint);
            }
        }

        return documentDistData;
    }

    public List<Map<String, Object>> getSimilarTopicsData() {
        List<Map<String, Object>> topicsData = new ArrayList<>();

        if (similarTopics != null && !similarTopics.isEmpty()) {
            for (Object[] topicData : similarTopics) {
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("topic", topicData[0]);
                dataPoint.put("overlap", topicData[1]);
                topicsData.add(dataPoint);
            }
        }

        return topicsData;
    }
}
