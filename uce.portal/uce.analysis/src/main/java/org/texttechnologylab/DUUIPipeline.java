package org.texttechnologylab;

import com.google.gson.Gson;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.hucompute.textimager.uima.type.category.CategoryCoveredTagged;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIRemoteDriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.type.LLMPrompt;
import org.texttechnologylab.type.LLMSystemPrompt;
import org.texttechnologylab.typeClasses.*;
import org.texttechnologylab.annotation.*;
import org.texttechnologylab.modules.ModelInfo;
import org.texttechnologylab.modules.Sentences;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.bson.Document;
import java.util.Map;
import org.texttechnologylab.type.LLMResult;
import org.texttechnologylab.type.LLMPrompt;

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
            );
        }
        return composer;
    }

    public DUUIComposer setComposer(HashMap<String, ModelInfo> urls) throws URISyntaxException, IOException, UIMAException, SAXException, CompressorException {
        DUUIComposer composer;
        composer = new DUUIComposer()
                .withSkipVerification(true)
                .withLuaContext(new DUUILuaContext().withJsonLibrary());

        DUUIRemoteDriver remoteDriver = new DUUIRemoteDriver();
        composer.addDriver(remoteDriver);
        for (Map.Entry<String, ModelInfo> url : urls.entrySet()) {
            String Variant = url.getValue().getVariant();
            switch (Variant){
                case "Coherence":
                    composer.add(
                            new DUUIRemoteDriver.Component(url.getValue().getUrl())
                                    .withParameter("selection", "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence")
                                    .withParameter("model_name", url.getValue().getMap())
                                    .withParameter("complexity_compute", "euclidean,cosine,wasserstein,distance,jensenshannon,bhattacharyya")
                                    .withParameter("model_art", url.getValue().getModelType())
                                    .withParameter("embeddings_keep", "0")
                    );
                    break;
                case "Stance":
                    composer.add(
                            new DUUIRemoteDriver.Component(url.getValue().getUrl())
                                    .withParameter("chatgpt_key", "")
                    );
                    break;
                case "LLM":
                    composer.add(
                            new DUUIRemoteDriver.Component(url.getValue().getUrl())
                                    .withParameter("seed", "42")
                                    .withParameter("model_name", url.getValue().getMap())
                                    .withParameter("url", url.getValue().getUrlParameter())
                                    .withParameter("temperature", "1")
                                    .withParameter("port", url.getValue().getPortParameter())
                    );
                    break;
                default:
                    composer.add(
                            new DUUIRemoteDriver.Component(url.getValue().getUrl())
                                    .withParameter("selection", "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence")
                    );
                    break;
            }

        }
        return composer;
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

    public Object[] setClaimFact(JCas cas, String claim, StringBuilder sb) throws UIMAException {
        Claim claimAnnotation = new Claim(cas, sb.length(), sb.length()+claim.length());
        sb.append(claim).append(" ");
        Collection<Sentence> sentences = JCasUtil.select(cas, Sentence.class);
        int length = sentences.size();
        claimAnnotation.setFacts(new FSArray(cas, length));
        int counter = 0;
        for (Sentence sentence : sentences) {
            // Create a new Fact annotation for each sentence
            int begin = sentence.getBegin();
            int end = sentence.getEnd();
            Fact factAnnotation = new Fact(cas, begin, end);
            factAnnotation.setClaims(new FSArray(cas, 1));
            factAnnotation.setClaims(0, claimAnnotation);
            factAnnotation.addToIndexes();
            claimAnnotation.setFacts(counter, factAnnotation);
            counter++;
        }
        claimAnnotation.addToIndexes();
        return new Object[]{cas, sb};
    }

    public Object[] setSentenceComparisons(JCas cas, String sentence1, StringBuilder sb) throws UIMAException {
        Annotation sentenceAnnotation1 = new Annotation(cas, sb.length(), sb.length()+sentence1.length());
        sb.append(sentence1).append(" ");
        sentenceAnnotation1.addToIndexes();
        Collection<Sentence> sentences = JCasUtil.select(cas, Sentence.class);
        for (Sentence sentence : sentences) {
            // Create a new Sentence annotation for each sentence
            SentenceComparison sentenceComparison = new SentenceComparison(cas);
            sentenceComparison.setSentenceI(sentenceAnnotation1);
            sentenceComparison.setSentenceJ(sentence);
            sentenceComparison.addToIndexes();
        }
        return new Object[]{cas, sb};
    }

    public Object[] setStance(JCas cas, String hypothesis, StringBuilder sb) throws UIMAException{
        Hypothesis hypothesisAnnotation = new Hypothesis(cas, sb.length(), sb.length()+hypothesis.length());
        sb.append(hypothesis).append(" ");
        Collection<Sentence> sentences = JCasUtil.select(cas, Sentence.class);
        int length = sentences.size();
        hypothesisAnnotation.setStances(new FSArray(cas, length));
        int counter = 0;
        for (Sentence sentence : sentences) {
            // Create a new Stance annotation for each sentence
            int begin = sentence.getBegin();
            int end = sentence.getEnd();
            StanceSentence stanceSentence = new StanceSentence(cas, begin, end);
            stanceSentence.addToIndexes();
            hypothesisAnnotation.setStances(counter, stanceSentence);
            counter++;
        }
        hypothesisAnnotation.addToIndexes();
        return new Object[]{cas, sb};
    }

    public Object[] setPrompt(JCas cas, String systemPrompt ,StringBuilder sb) throws UIMAException {
        int firstBegin = 0;
        int lastEnd = sb.length();
        LLMPrompt promptAnnotation = new LLMPrompt(cas, firstBegin, lastEnd);
        String promptText = sb.toString();
        promptAnnotation.setPrompt(promptText);
        if (systemPrompt != "") {
            LLMSystemPrompt systemPromptAnnotation = new LLMSystemPrompt(cas, sb.length(), sb.length()+systemPrompt.length());
            sb.append(systemPrompt).append(" ");
            systemPromptAnnotation.setMessage(systemPrompt);
            systemPromptAnnotation.addToIndexes();
            promptAnnotation.setSystemPrompt(systemPromptAnnotation);
        }
        promptAnnotation.addToIndexes();
        return new Object[]{cas, sb};
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
        textClass.computeAVGFact();
        textClass.computeAVGCoherence();
        textClass.computeAVGStance();
        textClass.computeAVGOffensive();
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
            case "Offensive":
                Collection<OffensiveSpeech> allOffensive = JCasUtil.select(cas, OffensiveSpeech.class);
                for (OffensiveSpeech offensive : allOffensive) {
                    OffensiveClass offensiveClass = new OffensiveClass();
                    int begin = offensive.getBegin();
                    int end = offensive.getEnd();
                    FSArray<AnnotationComment> offensives_all = offensive.getOffensives();
                    String model_name = offensive.getModel().getModelName();
                    offensiveClass.setModelInfo(modelInfo);
                    // model_name must be the same as modelInfo.getmap
                    if (!model_name.equals(modelInfo.getMap())) {
                        continue;
                    }
                    for (AnnotationComment annotationComment: offensives_all) {
                        String keyOffensive = annotationComment.getKey();
                        String valueOffensive = annotationComment.getValue();
                        OffensiveInput offensiveInput = new OffensiveInput();
                        offensiveInput.setKey(keyOffensive);
                        offensiveInput.setScore(Double.parseDouble(valueOffensive));
                        offensiveClass.addOffensive(offensiveInput);
                    }
                    sentences.getSentence(Integer.toString(begin), Integer.toString(end)).addOffensive(offensiveClass);
                    textClass.addOffensive(modelInfo, offensiveClass);
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
            case "Factchecking":
                Collection<FactChecking> factChecks = JCasUtil.select(cas, FactChecking.class);
                for(FactChecking factCheck : factChecks) {
                    Claim claim = factCheck.getClaim();
                    Fact fact = factCheck.getFact();
                    String model_name = factCheck.getModel().getModelName();
                    if (!model_name.equals(modelInfo.getMap())) {
                        continue;
                    }
                    int beginClaim = claim.getBegin();
                    int endClaim = claim.getEnd();
                    String claimText = claim.getCoveredText();
                    ClaimClass claimClass = new ClaimClass();
                    claimClass.setBegin(beginClaim);
                    claimClass.setEnd(endClaim);
                    claimClass.setClaim(claimText);
                    int beginFact = fact.getBegin();
                    int endFact = fact.getEnd();
                    FactClass factClass = new FactClass();
                    factClass.setModelInfo(modelInfo);
                    factClass.setFact(factCheck.getConsistency());
                    factClass.setNonFact(1 - factCheck.getConsistency());
                    factClass.setClaim(claimClass);
                    sentences.getSentence(Integer.toString(beginFact), Integer.toString(endFact)).addFact(factClass);
                    textClass.addFact(modelInfo, factClass);
                    textClass.setClaim(claimClass);
                }
                break;
            case "Coherence":
                HashMap<String, CoherenceClass> coherenceMap = new HashMap<>();
                Collection<Complexity> allcomplex = JCasUtil.select(cas, Complexity.class);
                for (Complexity complexity : allcomplex) {
                    String model_name = complexity.getModel().getModelName();
                    if (!model_name.equals(modelInfo.getMap())) {
                        continue;
                    }
                    Annotation sentenceI = complexity.getSentenceI();
                    Annotation sentenceJ = complexity.getSentenceJ();
                    int beginI = sentenceI.getBegin();
                    int endI = sentenceI.getEnd();
                    int beginJ = sentenceJ.getBegin();
                    int endJ = sentenceJ.getEnd();
                    String sentenceIText = sentenceI.getCoveredText();
                    String specKey = beginI + "_" + endI + "_" + beginJ + "_" + endJ + "_" + model_name;
                    // speckey not in CoherenceMap
                    if (!(coherenceMap.containsKey(specKey))){
                        CoherenceClass coherenceClass = new CoherenceClass();
                        coherenceClass.setModelInfo(modelInfo);
                        coherenceMap.put(specKey, coherenceClass);
                        CoherenceSentence coherenceSentence = new CoherenceSentence();
                        coherenceSentence.setSentence(sentenceIText);
                        coherenceSentence.setBegin(beginI);
                        coherenceSentence.setEnd(endI);
                        coherenceClass.setCoherenceSentence(coherenceSentence);
                        coherenceClass.setBegin(beginJ);
                        coherenceClass.setEnd(endJ);
                    }
                    CoherenceClass coherenceClass = coherenceMap.get(specKey);
                    String kind = complexity.getKind();
                    double value = complexity.getOutput();
                    switch (kind){
                        case "euclidean":
                            coherenceClass.setEuclidean(Float.parseFloat(String.valueOf(value)));
                            break;
                        case "cosine":
                            coherenceClass.setCosine(Float.parseFloat(String.valueOf(value)));
                            break;
                        case "wasserstein":
                            coherenceClass.setWasserstein(Float.parseFloat(String.valueOf(value)));
                            break;
                        case "distance":
                            coherenceClass.setDistanceCorrelation(Float.parseFloat(String.valueOf(value)));
                            break;
                        case "jensenshannon":
                            coherenceClass.setJensenshannon(Float.parseFloat(String.valueOf(value)));
                            break;
                        case "bhattacharyya":
                            coherenceClass.setBhattacharyya(Float.parseFloat(String.valueOf(value)));
                            break;
                    }
                }
                for (Map.Entry<String, CoherenceClass> entry : coherenceMap.entrySet()) {
                    CoherenceClass coherenceClass = entry.getValue();
                    int beginJ = coherenceClass.getBegin();
                    int endJ = coherenceClass.getEnd();
                    sentences.getSentence(Integer.toString(beginJ), Integer.toString(endJ)).addCoherence(coherenceClass);
                    textClass.addCoherence(modelInfo, coherenceClass);
                    textClass.setCoherenceSentence(coherenceClass.getCoherenceSentence());
                }
                break;
            case "Stance":
                Collection<Stance> stances_out = JCasUtil.select(cas, Stance.class);
                for (Stance stanceResult: stances_out) {
                    String model_name = stanceResult.getModel().getModelName();
                    if (!model_name.equals(modelInfo.getMap())) {
                        continue;
                    }
                    StanceSentence stanceSentence =(StanceSentence) stanceResult.getReference();
                    int beginStance = stanceSentence.getBegin();
                    int endStance = stanceSentence.getEnd();
                    StanceClass stanceClass = new StanceClass();
                    double oppose = stanceResult.getOppose();
                    double support = stanceResult.getSupport();
                    double neutral = stanceResult.getNeutral();
                    stanceClass.setModelInfo(modelInfo);
                    stanceClass.setOppose(oppose);
                    stanceClass.setSupport(support);
                    stanceClass.setNeutral(neutral);
                    sentences.getSentence(Integer.toString(beginStance), Integer.toString(endStance)).addStance(stanceClass);
                    textClass.addStance(modelInfo, stanceClass);
                }
                break;
            case "Readability":
                ReadabilityClass readabilityClass = new ReadabilityClass();
                for (CategoryCoveredTagged category : JCasUtil.select(cas, CategoryCoveredTagged.class)) {
                    String model_name = "Readability";
                    if (!model_name.equals(modelInfo.getMap())) {
                        continue;
                    }
                    String categoryName = category.getValue();
                    double categoryScore = category.getScore();
                    switch (categoryName) {
                        case "flesch_kincaid":
                            readabilityClass.setFleschKincaid(categoryScore);
                            break;
                        case "flesch":
                            readabilityClass.setFlesch(categoryScore);
                            break;
                        case "smog":
                            readabilityClass.setSMOG(categoryScore);
                            break;
                        case "dale_chall":
                            readabilityClass.setDaleChall(categoryScore);
                            break;
                        case "gunning_fog":
                            readabilityClass.setGunningFog(categoryScore);
                            break;
                        case "coleman_liau":
                            readabilityClass.setColemanLiau(categoryScore);
                            break;
                        case "ari":
                            readabilityClass.setARI(categoryScore);
                            break;
                        case "linsear_write":
                            readabilityClass.setLinsearWrite(categoryScore);
                            break;
                        case "spache":
                            readabilityClass.setSpache(categoryScore);
                            break;
                    }
                }
                readabilityClass.setModelInfo(modelInfo);
                textClass.addReadability(readabilityClass);
                break;
            case "TA":
                Collection<TAscore> taScores = JCasUtil.select(cas, TAscore.class);
                HashMap<String, TAClass> taScores_map = new HashMap<>();
                for (TAscore taScore : taScores) {
                    String name = taScore.getName();
                    String groupName = taScore.getGroup();
                    double score = taScore.getScore();
                    TAInput taInput = new TAInput();
                    taInput.setName(name);
                    taInput.setScore(score);
                    if (!taScores_map.containsKey(groupName)) {
                        TAClass taClass = new TAClass();
                        taClass.setGroupName(groupName);
                        taClass.setModelInfo(modelInfo);
                        taScores_map.put(groupName, taClass);
                    }
                    taScores_map.get(groupName).addTaInput(taInput);
                }
                for (Map.Entry<String, TAClass> entry : taScores_map.entrySet()) {
                    TAClass taClass = entry.getValue();
                    textClass.addAVGTA(taClass);
                }
                break;
            case "LLM":
                Collection<LLMResult> llmResults = JCasUtil.select(cas, LLMResult.class);
                for (LLMResult llmResult : llmResults) {
                    String content = llmResult.getContent();
                    String output = "";
                    if (content.contains("<think>")&&content.contains("</think>")) {
                        output = content.split("</think>")[1].trim();
                    }
                    else{
                        output = content;
                    }
                    // \n to html line break
                    output = output.replace("\n", "<br>");
                    LLMPrompt llmPrompt = llmResult.getPrompt();
                    String json_result = llmResult.getResult();
                    String json_metadata = llmResult.getMeta();
                    // String to Json
                    Gson gson = new Gson();
                    Document resultDocument = gson.fromJson(json_result, Document.class);
                    Document metadataDocument = gson.fromJson(json_metadata, Document.class);
                    double duration = metadataDocument.get("duration", Double.class);
                    String modelName = metadataDocument.get("model_name", String.class);
                    String systemPrompt = llmPrompt.getSystemPrompt().getMessage();
                    LLMClass llmClass = new LLMClass();
                    llmClass.setDuration(duration);
                    llmClass.setSystemPrompt(systemPrompt);
                    llmClass.setModelName(modelName);
                    llmClass.setResult(output);
                    llmClass.setModelInfo(modelInfo);
                    textClass.addLLM(modelInfo, llmClass);
                    textClass.addAVGLLM(llmClass);
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
