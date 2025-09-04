package org.texttechnologylab.uce.analysis;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.uce.analysis.modules.*;
import org.texttechnologylab.uce.analysis.typeClasses.TextClass;

import java.util.*;


public class RunDUUIPipeline {


    public DUUIInformation getModelResources(List<String> modelGroups, String inputText, String claim, String coherenceText, String stanceText, String systemPrompt) throws Exception {
        ModelResources modelResources = new ModelResources();
        List<ModelGroup> modelGroupsList = modelResources.getGroupedModelObjects();
        HashMap<String, ModelInfo> modelInfos = modelResources.getGroupMap();
        HashMap<String, ModelInfo> modelInfosMap = new HashMap<>();
        HashMap<String, String> urls = new HashMap<>();
        List<ModelInfo> modelInfosList = new ArrayList<>();
        List<String> ttlabScorerGroups = new ArrayList<>();
        List<String> cohmetrixScorerGroups = new ArrayList<>();
        boolean isHateSpeech = false;
        boolean isSentiment = false;
        boolean isTopic = false;
        boolean isToxic = false;
        boolean isEmotion = false;
        boolean isFact = false;
        boolean isCoherence = false;
        boolean specialModel = false;
        boolean isStance = false;
        boolean isReadability = false;
        boolean isLLM = false;
        boolean isTA = false;
        boolean isOffensive = false;
        boolean isTtlabScorer = false;
        boolean isCohmetrix = false;
        TTLabScorerInfo ttlabModelInfo = new TTLabScorerInfo();
        List<String> ttlabModelGroupNames = new ArrayList<>();
        List<String> cohmetrixGroups = new ArrayList<>();
        LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>> taInputMap = ttlabModelInfo.getTaInputMap();
        LinkedHashMap<String, LinkedHashMap<String, String>> taNameMap = ttlabModelInfo.getTAMapNames();
        LinkedHashMap<String, String> ttlabSubModels = taNameMap.get("submodels");
        LinkedHashMap<String, String> ttlabProperties = taNameMap.get("properties");
        CohMetrixInfo cohmetrixModelInfo = new CohMetrixInfo();
        LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>> cohmetrixInputMap = cohmetrixModelInfo.getCohMetrixMap();
        LinkedHashMap<String, LinkedHashMap<String, String>> cohmetrixNameMap = cohmetrixModelInfo.getCohMetrixMapInfo();
        LinkedHashMap<String, String> cohmetrixModels = cohmetrixNameMap.get("Models");
        LinkedHashMap<String, String> cohmetrixDescription = cohmetrixNameMap.get("Description");
        for (String modelKey : modelGroups) {
            if (modelInfos.containsKey(modelKey)) {
                ModelInfo modelInfo = modelInfos.get(modelKey);
                String url = modelInfo.getUrl();
                String name = modelInfo.getName();
                String Variant = modelInfo.getVariant();
                urls.put(name, url);
                modelInfosList.add(modelInfo);
                modelInfosMap.put(modelKey, modelInfo);
                switch (Variant) {
                    case "Hate":
                        isHateSpeech = true;
                        break;
                    case "Sentiment":
                        isSentiment = true;
                        break;
                    case "Topic":
                        isTopic = true;
                        break;
                    case "Toxic":
                        isToxic = true;
                        break;
                    case "Emotion":
                        isEmotion = true;
                        break;
                    case "Factchecking":
                        isFact = true;
                        specialModel = true;
                        break;
                    case "Coherence":
                        isCoherence = true;
                        specialModel = true;
                        break;
                    case "Stance":
                        isStance = true;
                        specialModel = true;
                        break;
                    case "Readability":
                        isReadability = true;
                        break;
                    case "LLM":
                        specialModel = true;
                        isLLM = true;
                        break;
                    case "TA":
                        isTA = true;
                        break;
                    case "Offensive":
                        isOffensive = true;
                        break;
                }
            }
            if (modelKey.startsWith("ttlabscorer##")) {
                String property = modelKey.replace("ttlabscorer##", "");
                ttlabScorerGroups.add(property);
                String ttlabModelGroupName = ttlabProperties.get(property);
                String ttlabSubModelName = ttlabSubModels.get(ttlabModelGroupName);
                if (!ttlabModelGroupNames.contains(ttlabSubModelName)){
                    ttlabModelGroupNames.add(ttlabSubModelName);
                    ModelInfo ttlabmodelInfo = ttlabModelInfo.getModelInfo();
                    ttlabmodelInfo.setName(ttlabSubModelName);
                    ttlabmodelInfo.setMainTool("TTLAB Scorer");
                    ttlabmodelInfo.setKey(ttlabSubModelName);
                    modelInfosList.add(ttlabmodelInfo);
                    String modelKeyName = ttlabmodelInfo.getMainTool().replace(" ", "_")+"_"+ttlabmodelInfo.getKey().replace(" ", "_");
                    modelInfosMap.put(modelKeyName, ttlabmodelInfo);
                }
                isTtlabScorer = true;
            }
            if(modelKey.startsWith("cohmetrix##")){
                String labelName = modelKey.replace("cohmetrix##", "");
                cohmetrixScorerGroups.add(labelName);
                String cohmetrixModelGroupName = cohmetrixModels.get(labelName);
                String cohmetrixModelDescription = cohmetrixDescription.get(labelName);
                if (!cohmetrixGroups.contains(cohmetrixModelGroupName)) {
                    cohmetrixGroups.add(cohmetrixModelGroupName);
                    ModelInfo cohmetrixmodel= cohmetrixModelInfo.getModelInfo();
                    cohmetrixmodel.setName(cohmetrixModelGroupName);
                    cohmetrixmodel.setMainTool("CohMetrix");
                    cohmetrixmodel.setKey(cohmetrixModelGroupName);
                    modelInfosList.add(cohmetrixmodel);
                    String modelKeyName = cohmetrixmodel.getMainTool().replace(" ", "_")+"_"+cohmetrixmodel.getKey().replace(" ", "_");
                    modelInfosMap.put(modelKeyName, cohmetrixmodel);
                }
                isCohmetrix = true;
            }
        }
        DUUIPipeline pipeline = new DUUIPipeline();
        // Add language detection
        JCas cas = pipeline.getLanguage(inputText);
        // get cas sentences

        cas = pipeline.getSentences(cas);
        if (specialModel) {
            // get cas sentences for coherence
            JCas newCas = JCasFactory.createJCas();
            String language = cas.getDocumentLanguage();
            newCas.setDocumentLanguage(language);
            String text = cas.getDocumentText();
            StringBuilder sb = new StringBuilder();
            sb.append(text).append(" ");
            // set document sentences
            Collection<Sentence> allSentences = JCasUtil.select(cas, Sentence.class);
            for (Sentence sentence : allSentences) {
                int begin = sentence.getBegin();
                int end = sentence.getEnd();
                Sentence newSentence = new Sentence(newCas);
                newSentence.setBegin(begin);
                newSentence.setEnd(end);
                newSentence.addToIndexes();
            }
            if (isFact) {
                Object[] output_fact = pipeline.setClaimFact(newCas, claim, sb);
                newCas = (JCas) output_fact[0];
                sb = (StringBuilder) output_fact[1];
            }
            // Coherence
            if (isCoherence) {
                Object[] output_coherence = pipeline.setSentenceComparisons(newCas, coherenceText, sb);
                newCas = (JCas) output_coherence[0];
                sb = (StringBuilder) output_coherence[1];
            }
            // Stance
            if (isStance) {
                Object[] output_stance = pipeline.setStance(newCas, stanceText, sb);
                newCas = (JCas) output_stance[0];
                sb = (StringBuilder) output_stance[1];
            }
            // LLM
            if (isLLM) {
                Object[] output_llm = pipeline.setPrompt(newCas, systemPrompt, sb);
                newCas = (JCas) output_llm[0];
                sb = (StringBuilder) output_llm[1];
            }
            text = sb.toString();
            // set document text
            newCas.setDocumentText(text);
            cas = newCas;

        }
        // run pipeline
        DUUIComposer composer = pipeline.setComposer(modelInfosMap);
        JCas result = pipeline.runPipeline(cas, composer);
        // get results
        Object[] results = pipeline.getJCasResults(result, modelInfosList, ttlabScorerGroups, cohmetrixScorerGroups);
        // print results
        Sentences sentences = (Sentences) results[0];
        TextClass textClass = (TextClass) results[1];
        DUUIInformation duuiInformation = new DUUIInformation(sentences, textClass, modelGroupsList, modelInfos);
        // set hate speech
        duuiInformation.setIsHateSpeech(isHateSpeech);
        // set sentiment
        duuiInformation.setIsSentiment(isSentiment);
        // set topic
        duuiInformation.setIsTopic(isTopic);
        // set toxic
        duuiInformation.setIsToxic(isToxic);
        // set emotion
        duuiInformation.setIsEmotion(isEmotion);
        // set fact
        duuiInformation.setIsFact(isFact);
        // set coherence
        duuiInformation.setIsCoherence(isCoherence);
        // set stance
        duuiInformation.setIsStance(isStance);
        // set readability
        duuiInformation.setIsReadability(isReadability);
        // set LLM
        duuiInformation.setIsLLM(isLLM);
        // set TA
        duuiInformation.setIsTA(isTA);
        // set offensive
        duuiInformation.setIsOffensive(isOffensive);
        // set ttlab scorer
        duuiInformation.setIsTtlabScorer(isTtlabScorer);
        if (isTtlabScorer) {
            duuiInformation.setTtlabScorerGroups(ttlabScorerGroups);
        }
        duuiInformation.setIsCohMetrix(isCohmetrix);
        if (isCohmetrix) {
            duuiInformation.setCohMetrixGroups(cohmetrixScorerGroups);
        }
        return duuiInformation;
    }

    public static void main(String[] args) throws Exception {
        ModelResources modelResources = new ModelResources();
        List<ModelGroup> modelGroups = modelResources.getGroupedModelObjects();
        List<ModelInfo> modelInfos = new ArrayList<>();
        List<String> modelGroupNames = new ArrayList<>();
        for (ModelGroup modelGroup : modelGroups) {
            List<ModelInfo> models = modelGroup.getModels();
            for (ModelInfo model : models) {
                String key = model.getKey();
                String mainTool = model.getMainTool();
                modelGroupNames.add(mainTool.replace(" ", "_") + "_" + key);
            }
        }
        String inputText = "Das ist ein Text, welches über Sport und Fußball handelt. Der Fußball Lionel Messi hat in der 25min. ein Tor gegen Real Madrid geschossen! Dadruch hat Barcelona gewonnen.";
        String claim = "Lionel Messi hat ein Tor geschossen";
        String coherenceText = "Das ist ein Text, welches über Sport und Fußball handelt. Der Fußball Lionel Messi hat in der 25min. ein Tor gegen Real Madrid geschossen! Dadruch hat Barcelona gewonnen.";
        String stanceText = "The author of this tweet {} Trump.";
        String systemPrompt = "You are a helpful assistant that analyzes text and provides insights based on the provided models.";
        DUUIInformation duuiInformation = new RunDUUIPipeline().getModelResources(modelGroupNames, inputText, claim, coherenceText, stanceText, systemPrompt);

    }

}
