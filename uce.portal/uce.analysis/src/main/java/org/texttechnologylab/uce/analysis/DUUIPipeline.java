package org.texttechnologylab.uce.analysis;

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
import org.bson.Document;
import org.hucompute.textimager.uima.type.category.CategoryCoveredTagged;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIRemoteDriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.annotation.*;
import org.texttechnologylab.type.LLMPrompt;
import org.texttechnologylab.type.LLMResult;
import org.texttechnologylab.type.LLMSystemPrompt;
import org.texttechnologylab.uce.analysis.modules.CohMetrixInfo;
import org.texttechnologylab.uce.analysis.modules.ModelInfo;
import org.texttechnologylab.uce.analysis.modules.Sentences;
import org.texttechnologylab.uce.analysis.modules.TTLabScorerInfo;
import org.texttechnologylab.uce.analysis.typeClasses.*;
import org.texttechnologylab.uima.type.cohmetrix.Index;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

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
        ArrayList<String> alreadyAddedUrl = new ArrayList<>();
        for (Map.Entry<String, ModelInfo> url : urls.entrySet()) {
            if (!alreadyAddedUrl.contains(url.getValue().getUrl())) {
                String Variant = url.getValue().getVariant();
                alreadyAddedUrl.add(url.getValue().getUrl());
                switch (Variant) {
                    case "cohmetrix":
                        composer.add(new DUUIRemoteDriver.Component(url.getValue().getUrl()));
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
        spacyUrls.put("Spacy", "http://spacy-cohmetrix.service.component.duui.texttechnologylab.org");
//        spacyUrls.put("Spacy", "http://spacy.service.component.duui.texttechnologylab.org");
        spacyUrls.put("Syntok", "http://paragraph-syntok.service.component.duui.texttechnologylab.org/");
        DUUIComposer composer = setListComposer(spacyUrls);
        cas = runPipeline(cas, composer);
        // Iterate over the sentences
//        Collection<Sentence> sentences = JCasUtil.select(cas, Sentence.class);
//        for (Sentence sentence : sentences) {
//            System.out.println("Sentence: " + sentence.getCoveredText());
//            System.out.println("Begin: " + sentence.getBegin());
//            System.out.println("End: " + sentence.getEnd());
//        }
//        Collection< Paragraph > paragraphs = JCasUtil.select(cas, Paragraph.class);
//        for (Paragraph paragraph : paragraphs) {
//            System.out.println("Paragraph: "+ paragraph.getCoveredText());
//            System.out.println("Begin Paragraph"+ paragraph.getBegin());
//            System.out.println("End Paragraph"+ paragraph.getEnd());
//        }
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

    public Object[] getJCasResults(JCas cas, List<ModelInfo> modelGroups, List<String> ttlabScorerGroups, List<String> cohMetrixScorerGroups) throws UIMAException, ResourceInitializationException, CASException, IOException, SAXException, CompressorException {
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
            Object [] extractedResults = getExtractedResults(cas, modelGroup, sentences, textClass, ttlabScorerGroups, cohMetrixScorerGroups);
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

    public Object[] getExtractedResults(JCas cas, ModelInfo modelInfo, Sentences sentences, TextClass textClass, List<String> ttlabScorerGroups, List<String> cohMetrixScorerGroups) throws UIMAException, ResourceInitializationException, CASException, IOException, SAXException, CompressorException {
        String btName = "Bert Token";
        String ACName = "Auto Correlation";
        TTLabScorerInfo ttLabScorerInfo = new TTLabScorerInfo();
        CohMetrixInfo cohMetrixInfo = new CohMetrixInfo();
        switch (modelInfo.getVariant()) {
            case "cohmetrix":
                List<String> UsedCohMetrix = new ArrayList<>();
                Collection<Index> cohMetrixScores = JCasUtil.select(cas, Index.class);
                LinkedHashMap<String, CohMetrixClass> cohMetrixScores_map = new LinkedHashMap<>();
                LinkedHashMap<String, LinkedHashMap<String, String>> cohMetrixMapNames = cohMetrixInfo.getCohMetrixMapInfo();
                LinkedHashMap<String, String> cohMetrixModelNames = cohMetrixMapNames.get("Models");
                LinkedHashMap<String, String> cohMetrixDescription = cohMetrixMapNames.get("Description");
                for (Index index : cohMetrixScores) {
                    String groupname = index.getTypeName();
                    String labelv2 = index.getLabelV2();
                    String labelv3 = index.getLabelV3();
                    String ttlabName = index.getLabelTTLab();
                    String Description = index.getDescription();
                    double score = index.getValue();
                    String error = index.getError();
                    if (cohMetrixScorerGroups.contains(labelv3) || cohMetrixScorerGroups.contains(ttlabName)) {
                        String labelv3Name = cohMetrixModelNames.get(labelv3);
                        String ttlabNameMap = cohMetrixModelNames.get(ttlabName);
                        if (ttlabNameMap != null) {
                            if (!ttlabNameMap.equals(modelInfo.getName())) {
                                continue; // skip if model name does not match modelInfo name
                            }
                        }
                        if (labelv3Name != null) {
                            if (!labelv3Name.equals(modelInfo.getName())) {
                                continue; // skip if model name does not match modelInfo name
                            }
                        }
                        if (UsedCohMetrix.contains(ttlabName) || UsedCohMetrix.contains(labelv3)) {
                            continue; // skip if already used
                        }
                        if (ttlabName == null && !UsedCohMetrix.contains(labelv3)) {
                            UsedCohMetrix.add(labelv3);
                        }
                        else{
                            UsedCohMetrix.add(ttlabName);
                        }
                        CohMetrixInput cohMetrixInput = new CohMetrixInput();
                        if (ttlabName != null) {
                            cohMetrixInput.setName(ttlabName);
                        }
                        else {
                            cohMetrixInput.setName(labelv3);
                        }
                        cohMetrixInput.setScore(score);
                        cohMetrixInput.setDescription(Description);
                        if (!cohMetrixScores_map.containsKey(groupname)) {
                            CohMetrixClass cohMetrixClass = new CohMetrixClass();
                            cohMetrixClass.setModelInfo(modelInfo);
                            cohMetrixClass.setGroupName(groupname);
                            cohMetrixScores_map.put(groupname, cohMetrixClass);
                        }
                        cohMetrixScores_map.get(groupname).addCohMetrixInput(cohMetrixInput);
                    }
                }
                for (Map.Entry<String, CohMetrixClass> entry : cohMetrixScores_map.entrySet()) {
                    CohMetrixClass cohMetrixClass = entry.getValue();
                    textClass.addAVGCohMetrix(cohMetrixClass);
                }
            case "ttlabscorer":
                List<String> UsedSubmodels = new ArrayList<>();
                LinkedHashMap<String, TAClass> ttlabScores_map = new LinkedHashMap<>();
                Collection<TAscore> ttlabScores = JCasUtil.select(cas, TAscore.class);
                LinkedHashMap<String, LinkedHashMap<String, String>> taMapNames = ttLabScorerInfo.getTAMapNames();
                LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>> taInputMap = ttLabScorerInfo.getTaInputMap();
                for (TAscore taScore : ttlabScores) {
                    String name = taScore.getName();
                    String groupName = taScore.getGroup();
                    String nameWithoutPrefix = "";
                    double score = taScore.getScore();
                    if (ttlabScorerGroups.contains(name)) {
                        String groupNameMap = taMapNames.get("properties").get(name);
                        String labelName = taMapNames.get("labels").get(name);
                        String submodelNameMap = taMapNames.get("submodels").get(groupNameMap);
                        if (!submodelNameMap.equals(modelInfo.getName()))
                            continue; // skip if submodel name does not match modelInfo name
                        if (!UsedSubmodels.contains(submodelNameMap)) {
                            UsedSubmodels.add(submodelNameMap);
                        }
                        TAInput ttlabInput = new TAInput();
                        ttlabInput.setName(labelName);
                        ttlabInput.setScore(score);
                        if (!ttlabScores_map.containsKey(groupNameMap)) {
                            TAClass taClass = new TAClass();
                            taClass.setModelInfo(modelInfo);
                            taClass.setGroupName(groupNameMap);
                            ttlabScores_map.put(groupNameMap, taClass);
                        }
                        ttlabScores_map.get(groupNameMap).addTaInput(ttlabInput);
                    }
                }
                for (Map.Entry<String, TAClass> entry : ttlabScores_map.entrySet()) {
                    TAClass taClass = entry.getValue();
                    textClass.addAVGTA(taClass);
                }
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
            case "TA":
                Collection<TAscore> taScores = JCasUtil.select(cas, TAscore.class);
                LinkedHashMap<String, TAClass> taScores_map = new LinkedHashMap<>();
                boolean chosen = false;
                for (TAscore taScore : taScores) {
                    String name = taScore.getName();
                    String groupName = taScore.getGroup();
                    String nameWithoutPrefix = "";
                    double score = taScore.getScore();
                    boolean add = true;
                    switch (modelInfo.getName()) {
                        case "TTLAB Cohesion-l BERT Token Auto Correlation" -> {
                            switch (taScore.getName()) {
                                case String s when s.contains("btac"):
                                    nameWithoutPrefix = name.replace("btac", "");
                                    name = "lag " + nameWithoutPrefix;
                                    groupName = "Autocorrelation of BERT token probabilities (all subwords)";
                                    break;
                                case String s when s.contains("bt_prod_ac"):
                                    nameWithoutPrefix = name.replace("bt_prod_ac", "");
                                    name = "Bert Token Product Auto Correlation lag " + nameWithoutPrefix;
                                    groupName = "Autocorrelation of BERT token probabilities (aggregated via subword product)";
                                    break;
                                case String s when s.contains("bt_first_ac"):
                                    nameWithoutPrefix = name.replace("bt_first_ac", "");
                                    name = "lag " + nameWithoutPrefix;
                                    groupName = "Autocorrelation of BERT token probabilities (first subwords only)";
                                    break;
                                case String s when s.contains("bt_mean_ac"):
                                    nameWithoutPrefix = name.replace("bt_mean_ac", "");
                                    name = "lag " + nameWithoutPrefix;
                                    groupName ="Autocorrelation of BERT token probabilities (subword average aggregation)";
                                    break;
                                case String s when s.contains("bt_min_ac"):
                                    nameWithoutPrefix = name.replace("bt_min_ac", "");
                                    name = "lag " + nameWithoutPrefix;
                                    groupName ="Autocorrelation of BERT token probabilities (subword minimum aggregation)";
                                    break;
                                case String s when s.contains("bt_max_ac"):
                                    nameWithoutPrefix = name.replace("bt_max_ac", "");
                                    name = "lag " + nameWithoutPrefix;
                                    groupName ="Autocorrelation of BERT token probabilities (subword maximum aggregation)";
                                    break;
                                case String s when s.contains("btrac"):
                                    // _prod_ Product, _first_ First, _mean_ Mean, _min_ Min, _max_ Max
                                    nameWithoutPrefix = name.replace("btrac", "");
                                    name = "all subwords" + nameWithoutPrefix;
                                    groupName = "Recursive Autocorrelation of BERT token probabilities";
                                    break;
                                case String s when s.contains("bt_prod_rac"):
                                    nameWithoutPrefix = name.replace("bt_prod_rac", "");
                                    name = "aggregated via subword product" + nameWithoutPrefix;
                                    groupName = "Recursive Autocorrelation of BERT token probabilities";
                                    break;
                                case String s when s.contains("bt_first_rac"):
                                    nameWithoutPrefix = name.replace("bt_first_rac", "");
                                    name = "first subwords only" + nameWithoutPrefix;
                                    groupName = "Recursive Autocorrelation of BERT token probabilities";
                                    break;
                                case String s when s.contains("bt_mean_rac"):
                                    nameWithoutPrefix = name.replace("bt_mean_rac", "");
                                    name = "subword average aggregation" + nameWithoutPrefix;
                                    groupName = "Recursive Autocorrelation of BERT token probabilities";
                                    break;
                                case String s when s.contains("bt_min_rac"):
                                    nameWithoutPrefix = name.replace("bt_min_rac", "");
                                    name = "subword minimum aggregation" + nameWithoutPrefix;
                                    groupName = "Recursive Autocorrelation of BERT token probabilities";
                                    break;
                                case String s when s.contains("bt_max_rac"):
                                    nameWithoutPrefix = name.replace("bt_max_rac", "");
                                    name = "subword maximum aggregation" + nameWithoutPrefix;
                                    groupName = "Recursive Autocorrelation of BERT token probabilities";
                                    break;
                                default:
                                    add = false;
//                                    if (name.startsWith("bt") & !name.contains("adc")) {
//                                        groupName = "Other BERT Token Embedding Features";
//                                        add = true;
//                                    }
                                    break;
                            }
                        }
                        case "TTLAB Cohesion-l BERT Token Auto-Distance-Correlation" -> {
                            switch (taScore.getName()) {
                                case String s when s.contains("btadc"):
                                    nameWithoutPrefix = name.replace("btadc", "");
                                    name = "lag " + nameWithoutPrefix;
                                    groupName = "Auto Distance Correlation of BERT token probabilities (all subwords)";
                                    break;
                                case String s when s.contains("bt_prod_adc"):
                                    nameWithoutPrefix = name.replace("bt_prod_adc", "");
                                    name = "Bert Token Product Auto Correlation lag " + nameWithoutPrefix;
                                    groupName = "Auto Distance Correlation of BERT token probabilities (aggregated via subword product)";
                                    break;
                                case String s when s.contains("bt_first_adc"):
                                    nameWithoutPrefix = name.replace("bt_first_adc", "");
                                    name = "lag " + nameWithoutPrefix;
                                    groupName = "Auto Distance Correlation of BERT token probabilities (first subwords only)";
                                    break;
                                case String s when s.contains("bt_mean_adc"):
                                    nameWithoutPrefix = name.replace("bt_mean_adc", "");
                                    name = "lag " + nameWithoutPrefix;
                                    groupName ="Auto Distance Correlation of BERT token probabilities (subword average aggregation)";
                                    break;
                                case String s when s.contains("bt_min_adc"):
                                    nameWithoutPrefix = name.replace("bt_min_adc", "");
                                    name = "lag " + nameWithoutPrefix;
                                    groupName ="Auto Distance Correlation of BERT token probabilities (subword minimum aggregation)";
                                    break;
                                case String s when s.contains("bt_max_adc"):
                                    nameWithoutPrefix = name.replace("bt_max_adc", "");
                                    name = "lag " + nameWithoutPrefix;
                                    groupName ="Auto Distance Correlation of BERT token probabilities (subword maximum aggregation)";
                                    break;
                                case String s when s.contains("btradc"):
                                    // _prod_ Product, _first_ First, _mean_ Mean, _min_ Min, _max_ Max
                                    nameWithoutPrefix = name.replace("btradc", "");
                                    name = "all subwords" + nameWithoutPrefix;
                                    groupName = "Recursive Auto Distance Correlation of BERT token probabilities";
                                    break;
                                case String s when s.contains("bt_prod_radc"):
                                    nameWithoutPrefix = name.replace("bt_prod_radc", "");
                                    name = "aggregated via subword product" + nameWithoutPrefix;
                                    groupName = "Recursive Auto Distance Correlation of BERT token probabilities";
                                    break;
                                case String s when s.contains("bt_first_radc"):
                                    nameWithoutPrefix = name.replace("bt_first_radc", "");
                                    name = "first subwords only" + nameWithoutPrefix;
                                    groupName = "Recursive Auto Distance Correlation of BERT token probabilities";
                                    break;
                                case String s when s.contains("bt_mean_radc"):
                                    nameWithoutPrefix = name.replace("bt_mean_radc", "");
                                    name = "subword average aggregation" + nameWithoutPrefix;
                                    groupName = "Recursive Auto Distance Correlation of BERT token probabilities";
                                    break;
                                case String s when s.contains("bt_min_radc"):
                                    nameWithoutPrefix = name.replace("bt_min_radc", "");
                                    name = "subword minimum aggregation" + nameWithoutPrefix;
                                    groupName = "Recursive Auto Distance Correlation of BERT token probabilities";
                                    break;
                                case String s when s.contains("bt_max_radc"):
                                    nameWithoutPrefix = name.replace("bt_max_radc", "");
                                    name = "subword maximum aggregation" + nameWithoutPrefix;
                                    groupName = "Recursive Auto Distance Correlation of BERT token probabilities";
                                    break;
                                default:
                                    add = false;
                                    break;
                            }
                        }
                        case "TTLAB Syntactic Features" -> {
                            if (taScore.getGroup().equals("syntactic")) {
                                switch (taScore.getName()) {
                                    case String s when s.endsWith("mu"):
                                        groupName = "Syntactic Mean Aggregated Features";
                                        name = name.replace("mu", "");
                                        break;
                                    case String s when s.endsWith("H"):
                                        groupName = "Syntactic Entropy Aggregated Features";
                                        name = name.replace("H", "");
                                        break;
                                    case String s when s.endsWith("G"):
                                        groupName = "Syntactic Gini Coefficient Aggregated Features";
                                        name = name.replace("G", "");
                                        break;
                                    case String s when s.endsWith("rac"):
                                        groupName = "Syntactic Recursive Auto Correlation Aggregated Features";
                                        name = name.replace("rac", "");
                                        break;
                                    case String s when s.endsWith("adtw"):
                                        groupName = "Syntactic Auto Dynamic Time Warping Aggregated Features";
                                        name = name.replace("adtw", "");
                                        break;
                                    default:
                                        add = false;
                                        break;
                                }
                            }
                            else {
                                add = false;
                            }
                        }
                        case "TTLAB Cohesion-s BERT Sentence Auto Correlation & Auto-Distance-Correlation" -> {
                            switch (taScore.getName()) {
                                case String s when s.contains("bsac"):
                                    nameWithoutPrefix = name.replace("bsac", "");
                                    name = "lag " + nameWithoutPrefix;
                                    groupName = "Autocorrelation of BERT sentence probabilities";
                                    break;
                                case String s when s.contains("bsadc"):
                                    nameWithoutPrefix = name.replace("bsadc", "");
                                    name = "lag " + nameWithoutPrefix;
                                    groupName = "Auto Distance Correlation of BERT sentence probabilities";
                                    break;
                                case String s when s.contains("bsradc"):
                                    nameWithoutPrefix = name.replace("bsradc", "");
                                    name = "recursive" + nameWithoutPrefix;
                                    groupName = "Auto Distance Correlation of BERT sentence probabilities";
                                    break;
                                case String s when s.contains("bsrac"):
                                    // _prod_ Product, _first_ First, _mean_ Mean, _min_ Min, _max_ Max
                                    nameWithoutPrefix = name.replace("bsrac", "");
                                    name = "recursive" + nameWithoutPrefix;
                                    groupName = "Autocorrelation of BERT sentence probabilities";
                                    break;
                                default:
                                    add = false;
//                                    if (name.startsWith("bs") & !name.contains("adc")) {
//                                        groupName = "Other BERT Sentence Embedding Features";
//                                        add = true;
//                                    }
                                    break;
                            }
                        }
                        case "TTLAB Cohesion BERT Others" -> {
                            add = false;
                            if (name.startsWith("bs") & !name.contains("adc") & !name.contains("ac")) {
                                groupName = "Cohesion-s Other BERT Sentence Embedding Features";
                                add = true;
                            }
                            if (name.startsWith("bt") & !name.contains("adc") & !name.contains("ac")) {
                                groupName = "Cohesion-l Other BERT Token Embedding Features";
                                add = true;
                            }
                        }
                        case "TAScore" -> {
                            add = true;
                        }
                        default -> {
                            add = false;
                        }
                    }
                    if (add) {
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
                }
                for (Map.Entry<String, TAClass> entry : taScores_map.entrySet()) {
                    TAClass taClass = entry.getValue();
                    textClass.addAVGTA(taClass);
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
                LinkedHashMap<String, ReadabilityClass> readability_map = new LinkedHashMap<>();
                if (!("Readability (EN)".equals(modelInfo.getName()))) {
                    String model_name = modelInfo.getName();
                    Collection<ReadabilityAdvance> all_readability_advance = JCasUtil.select(cas, ReadabilityAdvance.class);
                    for (ReadabilityAdvance readabilityAdvance : all_readability_advance){
                        if (!modelInfo.getMap().equals(readabilityAdvance.getModel().getModelName())) {
                            continue;
                        }
                        String readability_group_name = readabilityAdvance.getGroupName();
                        String groupName_input = model_name + " " + readability_group_name;
                        ReadabilityClass readabilityAdvanceClass = new ReadabilityClass();
                        readabilityAdvanceClass.setModelInfo(modelInfo);
                        readabilityAdvanceClass.setGroupName(groupName_input);
                        FSArray<AnnotationComment> readability_sentences = readabilityAdvance.getTextReadabilities();
                        for (AnnotationComment comment_i : readability_sentences) {
                            String key = comment_i.getKey();
                            String value = comment_i.getValue();
                            ReadabilityInput readabilityInput = new ReadabilityInput();
                            readabilityInput.setName(key);
                            readabilityInput.setScore(Double.parseDouble(value));
                            readabilityAdvanceClass.addReadabilityInput(readabilityInput);
                        }
                        readability_map.put(groupName_input, readabilityAdvanceClass);
                    }
                    for (Map.Entry<String, ReadabilityClass> entry : readability_map.entrySet()) {
                        ReadabilityClass readabilityAdvanceClass = entry.getValue();
                        textClass.addReadability(readabilityAdvanceClass);
                    }
                }
                else{
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
