package org.texttechnologylab;
import org.apache.uima.jcas.JCas;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import java.util.*;


public class RunDUUIPipeline {


    public DUUIInformation getModelResources(List<String> modelGroups, String inputText, String claim, String coherenceText) throws Exception {
        ModelResources modelResources = new ModelResources();
        List<ModelGroup> modelGroupsList = modelResources.getGroupedModelObjects();
        HashMap<String, ModelInfo> modelInfos = modelResources.getGroupMap();
        HashMap<String, ModelInfo> modelInfosMap = new HashMap<>();
        HashMap<String, String> urls = new HashMap<>();
        List<ModelInfo> modelInfosList = new ArrayList<>();
        boolean isHateSpeech = false;
        boolean isSentiment = false;
        boolean isTopic = false;
        boolean isToxic = false;
        boolean isEmotion = false;
        boolean isFact = false;
        boolean isCoherence = false;
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
                        break;
                    case "Coherence":
                        isCoherence = true;
                        break;
                }
            }
        }
        DUUIPipeline pipeline = new DUUIPipeline();
        // Add language detection
        JCas cas = pipeline.getLanguage(inputText);
        // get cas sentences

        cas = pipeline.getSentences(cas);
        // if fact
        if (isFact) {
            cas = pipeline.setClaimFact(cas, claim);
        }
        if (isCoherence) {
            cas = pipeline.setSentenceComparisons(cas, coherenceText);
        }
        // run pipeline
        DUUIComposer composer = pipeline.setComposer(modelInfosMap);
        JCas result = pipeline.runPipeline(cas, composer);
        // get results
        Object[] results = pipeline.getJCasResults(result, modelInfosList);
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
        DUUIInformation duuiInformation = new RunDUUIPipeline().getModelResources(modelGroupNames, inputText, claim, coherenceText);

    }

}
