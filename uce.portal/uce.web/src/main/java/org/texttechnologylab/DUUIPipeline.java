package org.texttechnologylab;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import io.swagger.models.auth.In;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIRemoteDriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.annotation.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DUUIPipeline {

    public DUUIComposer setListComposer(HashMap<String, String> urls) throws URISyntaxException, IOException, UIMAException, SAXException, CompressorException {
        DUUIComposer composer;
        composer = new DUUIComposer()
                .withSkipVerification(true)
                .withLuaContext(new DUUILuaContext().withJsonLibrary());

        DUUIRemoteDriver remoteDriver = new DUUIRemoteDriver();
        composer.addDriver(remoteDriver);
        for (Map.Entry<String, String> url : urls.entrySet()) {
            composer.add(
                    new DUUIRemoteDriver.Component(url.getValue())
                            .withParameter("selection", "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence")
//                          .withTargetView(url.getKey())
            );
        }
        return composer;
    }

    public JCas createCas(String language, List<String> sentences) throws UIMAException {
        JCas cas = JCasFactory.createJCas();
        cas.setDocumentLanguage(language);

        StringBuilder sb = new StringBuilder();
        for (String sentence : sentences) {
            Sentence sentenceAnnotation = new Sentence(cas, sb.length(), sb.length()+sentence.length());
            sentenceAnnotation.addToIndexes();
            sb.append(sentence).append(" ");
        }

        cas.setDocumentText(sb.toString());
        return cas;
    }

    public JCas runPipeline(JCas cas, DUUIComposer composer) throws Exception {
        composer.run(cas);
        return cas;
    }



    public JCas getLanguage(String inputText) throws Exception {
        // Implement language detection logic here with DUUI
        JCas cas = JCasFactory.createJCas();
        cas.setDocumentText(inputText);
        HashMap<String, String> urls = new HashMap<>();
        urls.put("LanguageDetection", "http://language.service.component.duui.texttechnologylab.org");
        DUUIComposer composer = setListComposer(urls);
        cas = runPipeline(cas, composer);
        // Assuming the language detection component sets the language in the JCas
        String language = "en";
        language = cas.getDocumentLanguage();
        return cas;
    }

    public JCas getSentences(JCas cas) throws Exception {
        HashMap<String, String> spacyUrls = new HashMap<>();
        spacyUrls.put("Spacy", "http://spacy.service.component.duui.texttechnologylab.org");
        DUUIComposer composer = setListComposer(spacyUrls);
        cas = runPipeline(cas, composer);
        // Iterate over the sentences
        Collection<Sentence> sentences = JCasUtil.select(cas, Sentence.class);
        for (Sentence sentence : sentences) {
            System.out.println("Sentence: " + sentence.getCoveredText());
            System.out.println("Begin: " + sentence.getBegin());
            System.out.println("End: " + sentence.getEnd());
        }
        return cas;
    }

    public Object[] getJCasResults(JCas cas, List<ModelInfo> modelGroups) throws UIMAException, ResourceInitializationException, CASException, IOException, SAXException, CompressorException {
        Sentences sentences = new Sentences();
        // set sentences
        Collection<Sentence> allSentences = JCasUtil.select(cas, Sentence.class);
        for (Sentence sentence : allSentences) {
            int begin = sentence.getBegin();
            int end = sentence.getEnd();
            String text = sentence.getCoveredText();
            SentenceClass sentenceClass = new SentenceClass();
            sentenceClass.setBegin(begin);
            sentenceClass.setEnd(end);
            sentenceClass.setText(text);
            sentenceClass.setLanguage(cas.getDocumentLanguage());
            sentences.addSentence(Integer.toString(begin), Integer.toString(end), sentenceClass);
        }
        TextClass textClass = new TextClass();
        for (ModelInfo modelGroup : modelGroups) {
            Object [] extractedResults = getExtractedResults(cas, modelGroup, sentences, textClass);
            sentences = (Sentences) extractedResults[0];
            textClass = (TextClass) extractedResults[1];
        }
        textClass.computeAVGHate();
        textClass.computeAVGSentiment();
        textClass.computeAVGTopic();
        textClass.computeAVGToxic();
        textClass.computeAVGEmotion();
        return new Object[]{sentences, textClass};
    }

    public Object[] getExtractedResults(JCas cas, ModelInfo modelInfo, Sentences sentences, TextClass textClass) throws UIMAException, ResourceInitializationException, CASException, IOException, SAXException, CompressorException {
        switch (modelInfo.getVariant()) {
            case "Topic":
                Collection<Topic> allTopics = JCasUtil.select(cas, Topic.class);
                for (Topic topic : allTopics) {
                    TopicClass topicClass = new TopicClass();
                    int begin = topic.getBegin();
                    int end = topic.getEnd();
                    FSArray<AnnotationComment> topics_all = topic.getTopics();
                    String model_name = topic.getModel().getModelName();
                    topicClass.setModelInfo(modelInfo);
                    // model_name must be the same as modelInfo.getmap
                    if (!model_name.equals(modelInfo.getMap())) {
                        continue;
                    }
                    for (AnnotationComment annotationComment: topics_all) {
                        String keyTopic = annotationComment.getKey();
                        String valueTopic = annotationComment.getValue();
                        TopicInput topicInput = new TopicInput();
                        topicInput.setKey(keyTopic);
                        topicInput.setScore(Double.parseDouble(valueTopic));
                        topicClass.addTopic(topicInput);
                    }
                    sentences.getSentence(Integer.toString(begin), Integer.toString(end)).addTopic(topicClass);
                    textClass.addTopic(modelInfo, topicClass);
                }
                break;
            case "Emotion":
                Collection<Emotion> allEmotions = JCasUtil.select(cas, Emotion.class);
                for (Emotion emotion : allEmotions) {
                    EmotionClass emotionClass = new EmotionClass();
                    int begin = emotion.getBegin();
                    int end = emotion.getEnd();
                    FSArray<AnnotationComment> emotions_all = emotion.getEmotions();
                    String model_name = emotion.getModel().getModelName();
                    emotionClass.setModelInfo(modelInfo);
                    // model_name must be the same as modelInfo.getmap
                    if (!model_name.equals(modelInfo.getMap())) {
                        continue;
                    }
                    for (AnnotationComment annotationComment: emotions_all) {
                        String keyEmotion = annotationComment.getKey();
                        String valueEmotion = annotationComment.getValue();
                        EmotionInput emotionInput = new EmotionInput();
                        emotionInput.setKey(keyEmotion);
                        emotionInput.setScore(Double.parseDouble(valueEmotion));
                        emotionClass.addEmotion(emotionInput);
                    }
                    sentences.getSentence(Integer.toString(begin), Integer.toString(end)).addEmotion(emotionClass);
                    textClass.addEmotion(modelInfo, emotionClass);
                }
                break;
            case "Hate":
                Collection<Hate> allhates = JCasUtil.select(cas, Hate.class);
                for (Hate hate : allhates) {
                    String model_name = hate.getModel().getModelName();
                    if (!model_name.equals(modelInfo.getMap())) {
                        continue;
                    }
                    int begin = hate.getBegin();
                    int end = hate.getEnd();
                    HateClass hateClass = new HateClass();
                    hateClass.setModelInfo(modelInfo);
                    Double hateScore = hate.getHate();
                    Double nonHateScore = hate.getNonHate();
                    hateClass.setHate(hateScore);
                    hateClass.setNonHate(nonHateScore);
                    sentences.getSentence(Integer.toString(begin), Integer.toString(end)).addHate(hateClass);
                    textClass.addHate(modelInfo, hateClass);
                }
                break;
            case "Sentiment":
                Collection<SentimentModel> all_sentiments = JCasUtil.select(cas, SentimentModel.class);
                for (SentimentModel sentiment: all_sentiments) {
                    String model_name = sentiment.getModel().getModelName();
                    if (!model_name.equals(modelInfo.getMap())) {
                        continue;
                    }
                    Double negative = sentiment.getProbabilityNegative();
                    Double positive = sentiment.getProbabilityPositive();
                    Double neutral = sentiment.getProbabilityNeutral();
                    int begin = sentiment.getBegin();
                    int end = sentiment.getEnd();
                    SentimentClass sentimentClass = new SentimentClass();
                    sentimentClass.setModelInfo(modelInfo);
                    sentimentClass.setNegative(negative);
                    sentimentClass.setPositive(positive);
                    sentimentClass.setNeutral(neutral);
                    sentences.getSentence(Integer.toString(begin), Integer.toString(end)).addSentiment(sentimentClass);
                    textClass.addSentiment(modelInfo, sentimentClass);
                }
                break;
            case "Toxic":
                Collection<Toxic> allToxic = JCasUtil.select(cas, Toxic.class);
                for (Toxic toxicResult: allToxic){
                    String model_name = toxicResult.getModel().getModelName();
                    if (!model_name.equals(modelInfo.getMap())) {
                        continue;
                    }
                    int begin = toxicResult.getBegin();
                    int end = toxicResult.getEnd();
                    Double toxicScore = toxicResult.getToxic();
                    Double nonToxicScore = toxicResult.getNonToxic();
                    ToxicClass toxicClass = new ToxicClass();
                    toxicClass.setModelInfo(modelInfo);
                    toxicClass.setToxic(toxicScore);
                    toxicClass.setNonToxic(nonToxicScore);
                    sentences.getSentence(Integer.toString(begin), Integer.toString(end)).addToxic(toxicClass);
                    textClass.addToxic(modelInfo, toxicClass);
                }
                break;
        }
        return new Object[]{sentences, textClass};
    }


    // test methods
    public static void main(String[] args) throws Exception {
        DUUIPipeline pipeline = new DUUIPipeline();
        String inputText = "Das ist ein Text, welches über Sport und Fußball handelt. Der Fußball Lionel Messi hat in der 25min. ein Tor gegen Real Madrid geschossen! Dadruch hat Barcelona gewonnen.";
        JCas cas = pipeline.getLanguage(inputText);
        cas = pipeline.getSentences(cas);
        HashMap<String, String> urls = new HashMap<>();
        urls.put("Model1", "http://emotion-pysentimiento.service.component.duui.texttechnologylab.org");
//        urls.put("Model2", "http://model2.service.component.duui.texttechnologylab.org");
        DUUIComposer composer = pipeline.setListComposer(urls);
        JCas result = pipeline.runPipeline(cas, composer);
        System.out.println("Pipeline finished successfully.");
    }

}
