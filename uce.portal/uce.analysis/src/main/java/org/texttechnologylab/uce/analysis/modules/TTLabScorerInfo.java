package org.texttechnologylab.uce.analysis.modules;

import java.util.LinkedHashMap;

public class TTLabScorerInfo {

    String[] otherFeaturesSentences = {"bscdet", "bssimmu", "bssimH", "bsgd", "bsgrc", "bsgest", "bsgccmu", "bsgccH", "bsgclmu", "bsH", "bslH"};
    String[] otherFeaturesTokens = {"btdiffsum", "btdiffmu", "btdiffH", "btHa", "btlHa", "btH", "btlH", "btsH", "btlsH", "bth", "btdfa", "bt_prod_diffsum", "bt_prod_diffmu", "bt_prod_Ha", "bt_prod_lHa", "bt_prod_H", "bt_prod_lH", "bt_prod_sH", "bt_prod_lsH", "bt_prod_h", "bt_prod_dfa", "bt_first_diffsum", "bt_first_diffmu","bt_first_diffH", "bt_first_Ha", "bt_first_lHa", "bt_first_H", "bt_first_lH", "bt_first_sH", "bt_first_lsH", "bt_first_h", "bt_first_dfa", "bt_min_diffsum", "bt_min_diffmu", "bt_min_diffH", "bt_min_Ha", "bt_min_lHa", "bt_min_H", "bt_min_lH", "bt_min_sH", "bt_min_lsH", "bt_min_h", "bt_min_dfa", "bt_max_diffsum", "bt_max_diffmu", "bt_max_diffH", "bt_max_Ha", "bt_max_lHa", "bt_max_H", "bt_max_lH", "bt_max_sH", "bt_max_lsH", "bt_max_h", "bt_max_dfa", "bt_mean_diffsum", "bt_mean_diffmu", "bt_mean_diffH", "bt_mean_Ha", "bt_mean_lHa", "bt_mean_H", "bt_mean_lH", "bt_mean_sH", "bt_mean_lsH", "bt_mean_h", "bt_mean_dfa", "btgd_mahalanobis","btgrc_mahalanobis", "btgd_mahalanobis", "btgest_mahalanobis", "btdet_mahalanobis", "btgass_mahalanobis", "btglclmu_mahalanobis", "btglclr_mahalanobis", "btsr", "btmdiff", "btip"};

    public ModelInfo getModelInfo() {
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setName("TtlabScorer");
        modelInfo.setKey("ttlabscorer");
        modelInfo.setUrl("http://ta.service.component.duui.texttechnologylab.org");
        modelInfo.setGithub("https://github.com/mevbagci/duui-uima/tree/main/duui-TAScore");
        modelInfo.setHuggingface("");
        modelInfo.setPaper("");
        modelInfo.setMap("ttlabscorer");
        modelInfo.setVariant("ttlabscorer");
        modelInfo.setMainTool("ttlabscorer");
        modelInfo.setModelType("ttlabscorer");
        return modelInfo;
    }

    public LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>> getTaInputMap() {
        LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>> taInputMap = new LinkedHashMap<>();
        LinkedHashMap<String, LinkedHashMap<String, String>> taInput = new LinkedHashMap<>();
        LinkedHashMap<String, LinkedHashMap<String, String>> taInputAutoCorrelation = new LinkedHashMap<>();
        LinkedHashMap<String, LinkedHashMap<String, String>> taInputCohesionS = new LinkedHashMap<>();
        LinkedHashMap<String, LinkedHashMap<String, String>> taInputSyntaticFeatures = new LinkedHashMap<>();
        LinkedHashMap<String, LinkedHashMap<String, String>> taInputMapOther = new LinkedHashMap<>();
        String[] syntaticFeatures = {"LDE", "dep", "MDD", "SPE", "TCI", "imb", "L", "W", "w", "l", "c", "bc", "cc"};

        LinkedHashMap<String, String> AllScoresbtac = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScoresbt_prod_ac = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScoresbt_bt_first_ac = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScoresbt_bt_mean_ac = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScoresbt_bt_min_ac = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScoresbt_bt_max_ac = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScoresbt_btrac = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScores_btadc = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScores_bt_prod_adc = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScores_bt_first_adc = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScores_bt_mean_adc = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScores_bt_min_adc = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScores_bt_max_adc = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScores_bt_radc = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScores_bsac = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScores_bsadc = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScores_mu = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScores_H = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScores_G = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScores_rac = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScores_adtw = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScores_otherTokens = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScores_otherSentences = new LinkedHashMap<>();
        for (int i = 1; i < 111; i++) {
            String key = "lag" + i;
            AllScoresbtac.put(key, "btac" + i);
            AllScoresbt_prod_ac.put(key, "bt_prod_ac" + i);
            AllScoresbt_bt_first_ac.put(key, "bt_first_ac" + i);
            AllScoresbt_bt_mean_ac.put(key, "bt_mean_ac" + i);
            AllScoresbt_bt_min_ac.put(key, "bt_min_ac" + i);
            AllScoresbt_bt_max_ac.put(key, "bt_max_ac" + i);
            AllScores_btadc.put(key, "btadc" + i);
            AllScores_bt_prod_adc.put(key, "bt_prod_adc" + i);
            AllScores_bt_first_adc.put(key, "bt_first_adc" + i);
            AllScores_bt_mean_adc.put(key, "bt_mean_adc" + i);
            AllScores_bt_min_adc.put(key, "bt_min_adc" + i);
            AllScores_bt_max_adc.put(key, "bt_max_adc" + i);
        }
        for (int i = 1; i < 11; i++) {
            String key = "lag" + i;
            AllScores_bsac.put(key, "bsac" + i);
            AllScores_bsadc.put(key, "bsadc" + i);
        }
        for (String syntaticFeature : syntaticFeatures) {
            AllScores_mu.put(syntaticFeature, syntaticFeature+"mu");
            AllScores_H.put(syntaticFeature, syntaticFeature+"H");
            AllScores_G.put(syntaticFeature, syntaticFeature+"G");
            AllScores_rac.put(syntaticFeature, syntaticFeature+"rac");
            AllScores_adtw.put(syntaticFeature, syntaticFeature+"adtw");
        }
        for (String otherFeature : otherFeaturesTokens) {
            AllScores_otherTokens.put(otherFeature, otherFeature);
        }
        for (String  otherFeature: otherFeaturesSentences) {
            AllScores_otherSentences.put(otherFeature, otherFeature);
        }

        AllScores_bsac.put("recursive", "bsrac");
        AllScores_bsadc.put("recursive", "bsradc");

        AllScoresbt_btrac.put("all subwords", "btrac");
        AllScoresbt_btrac.put("aggregated via subword product", "bt_prod_rac");
        AllScoresbt_btrac.put("first subwords only", "bt_first_rac");
        AllScoresbt_btrac.put("subword average aggregation", "bt_mean_rac");
        AllScoresbt_btrac.put("subword minimum aggregation", "bt_min_rac");
        AllScoresbt_btrac.put("subword maximum aggregation", "bt_max_rac");
        AllScores_bt_radc.put("all subwords", "btradc");
        AllScores_bt_radc.put("aggregated via subword product", "bt_prod_radc");
        AllScores_bt_radc.put("first subwords only", "bt_first_radc");
        AllScores_bt_radc.put("subword average aggregation", "bt_mean_radc");
        AllScores_bt_radc.put("subword minimum aggregation", "bt_min_radc");
        AllScores_bt_radc.put("subword maximum aggregation", "bt_max_radc");

        taInput.put("Autocorrelation of BERT token probabilities (all subwords)", AllScoresbtac);
        taInput.put("Autocorrelation of BERT token probabilities (aggregated via subword product)", AllScoresbt_prod_ac);
        taInput.put("Autocorrelation of BERT token probabilities  (first subwords only)", AllScoresbt_bt_first_ac);
        taInput.put("Autocorrelation of BERT token probabilities (subword average aggregation)", AllScoresbt_bt_mean_ac);
        taInput.put("Autocorrelation of BERT token probabilities (subword minimum aggregation)", AllScoresbt_bt_min_ac);
        taInput.put("Autocorrelation of BERT token probabilities (subword maximum aggregation)", AllScoresbt_bt_max_ac);
        taInput.put("Recursive Autocorrelation of BERT token probabilities", AllScoresbt_btrac);

        taInputAutoCorrelation.put("Auto Distance Correlation of BERT token probabilities (all subwords)", AllScores_btadc);
        taInputAutoCorrelation.put("Auto Distance Correlation of BERT token probabilities (aggregated via subword product)", AllScores_bt_prod_adc);
        taInputAutoCorrelation.put("Auto Distance Correlation of BERT token probabilities (first subwords only)", AllScores_bt_first_adc);
        taInputAutoCorrelation.put("Auto Distance Correlation of BERT token probabilities (subword average aggregation)", AllScores_bt_mean_adc);
        taInputAutoCorrelation.put("Auto Distance Correlation of BERT token probabilities (subword minimum aggregation)", AllScores_bt_min_adc);
        taInputAutoCorrelation.put("Auto Distance Correlation of BERT token probabilities (subword maximum aggregation)", AllScores_bt_max_adc);
        taInputAutoCorrelation.put("Recursive Auto Distance Correlation of BERT token probabilities", AllScores_bt_radc);

        taInputCohesionS.put("Autocorrelation of BERT sentence probabilities", AllScores_bsac);
        taInputCohesionS.put("Auto Distance Correlation of BERT sentence probabilities", AllScores_bsadc);

        taInputSyntaticFeatures.put("Syntactic Mean Aggregated Features", AllScores_mu);
        taInputSyntaticFeatures.put("Syntactic Entropy Aggregated Features", AllScores_H);
        taInputSyntaticFeatures.put("Syntactic Gini Coefficient Aggregated Features", AllScores_G);
        taInputSyntaticFeatures.put("Syntactic Recursive Auto Correlation Aggregated Features", AllScores_rac);
        taInputSyntaticFeatures.put("Syntactic Auto Dynamic Time Warping Aggregated Features", AllScores_adtw);

        taInputMapOther.put("Cohesion-l Other BERT Token Embedding Features", AllScores_otherTokens);
        taInputMapOther.put("Cohesion-s Other BERT Sentence Embedding Features", AllScores_otherSentences);

        taInputMap.put("TTLAB Cohesion-l BERT Token Auto Correlation", taInput);
        taInputMap.put("TTLAB Cohesion-l BERT Token Auto Distance Correlation", taInputAutoCorrelation);
        taInputMap.put("TTLAB Cohesion-s BERT Sentence Auto Correlation & Auto-Distance-Correlation", taInputCohesionS);
        taInputMap.put("TTLAB Syntactic Features", taInputSyntaticFeatures);
        taInputMap.put("TTLAB Cohesion BERT Others", taInputMapOther);
        return taInputMap;
    }

    public LinkedHashMap<String, LinkedHashMap<String, String>> getTAMapNames() {
        LinkedHashMap<String, LinkedHashMap<String, String>> taMapNames = new LinkedHashMap<>();
        LinkedHashMap<String, String> taNames = new LinkedHashMap<>();
        LinkedHashMap<String, String> MapShortNames = new LinkedHashMap<>();
        String[] syntaticFeatures = {"LDE", "dep", "MDD", "SPE", "TCI", "imb", "L", "W", "w", "l", "c", "bc", "cc"};

        for (int i = 1; i < 111; i++) {
            taNames.put("btac" + i, "Autocorrelation of BERT token probabilities (all subwords)");
            taNames.put("bt_prod_ac" + i, "Autocorrelation of BERT token probabilities (aggregated via subword product)");
            taNames.put("bt_first_ac" + i, "Autocorrelation of BERT token probabilities (first subwords only)");
            taNames.put("bt_mean_ac" + i, "Autocorrelation of BERT token probabilities (subword average aggregation)");
            taNames.put("bt_min_ac" + i, "Autocorrelation of BERT token probabilities (subword minimum aggregation)");
            taNames.put("bt_max_ac" + i, "Autocorrelation of BERT token probabilities (subword maximum aggregation)");
            MapShortNames.put("btac" + i, "lag" + i);
            MapShortNames.put("bt_prod_ac" + i, "lag" + i);
            MapShortNames.put("bt_first_ac" + i, "lag" + i);
            MapShortNames.put("bt_mean_ac" + i, "lag" + i);
            MapShortNames.put("bt_min_ac" + i, "lag" + i);
            MapShortNames.put("bt_max_ac" + i, "lag" + i);

            taNames.put("btadc" + i, "Auto Distance Correlation of BERT token probabilities (all subwords)");
            taNames.put("bt_prod_adc" + i, "Auto Distance Correlation of BERT token probabilities (aggregated via subword product)");
            taNames.put("bt_first_adc" + i, "Auto Distance Correlation of BERT token probabilities (first subwords only)");
            taNames.put("bt_mean_adc" + i, "Auto Distance Correlation of BERT token probabilities (subword average aggregation)");
            taNames.put("bt_min_adc" + i, "Auto Distance Correlation of BERT token probabilities (subword minimum aggregation)");
            taNames.put("bt_max_adc" + i, "Auto Distance Correlation of BERT token probabilities (subword maximum aggregation)");
            MapShortNames.put("btadc" + i, "lag" + i);
            MapShortNames.put("bt_prod_adc" + i, "lag" + i);
            MapShortNames.put("bt_first_adc" + i, "lag" + i);
            MapShortNames.put("bt_mean_adc" + i, "lag" + i);
            MapShortNames.put("bt_min_adc" + i, "lag" + i);
            MapShortNames.put("bt_max_adc" + i, "lag" + i);
        }
        for (int i = 1; i < 11; i++) {
            taNames.put("bsac" + i, "Autocorrelation of BERT sentence probabilities");
            taNames.put("bsadc" + i, "Auto Distance Correlation of BERT sentence probabilities");
            MapShortNames.put("bsac" + i, "lag" + i);
            MapShortNames.put("bsadc" + i, "lag" + i);
        }
        MapShortNames.put("bsrac", "recursive");
        MapShortNames.put("bsradc", "recursive");
        taNames.put("bsrac", "Autocorrelation of BERT sentence probabilities");
        taNames.put("bsradc", "Auto Distance Correlation of BERT sentence probabilities");
        for(String syntaticFeature : syntaticFeatures) {
            taNames.put(syntaticFeature+"mu", "Syntactic Mean Aggregated Features");
            taNames.put(syntaticFeature+"H", "Syntactic Entropy Aggregated Features");
            taNames.put(syntaticFeature+"G", "Syntactic Gini Coefficient Aggregated Features");
            taNames.put(syntaticFeature+"rac", "Syntactic Recursive Auto Correlation Aggregated Features");
            taNames.put(syntaticFeature+"adtw", "Syntactic Auto Dynamic Time Warping Aggregated Features");

            MapShortNames.put(syntaticFeature+"mu", syntaticFeature);
            MapShortNames.put(syntaticFeature+"H", syntaticFeature);
            MapShortNames.put(syntaticFeature+"G", syntaticFeature);
            MapShortNames.put(syntaticFeature+"rac", syntaticFeature);
            MapShortNames.put(syntaticFeature+"adtw", syntaticFeature);
        }
        for (String otherFeature : otherFeaturesSentences) {
            taNames.put(otherFeature, "Cohesion-s Other BERT Sentence Embedding Features");
            MapShortNames.put(otherFeature, otherFeature);
        }
        for (String otherFeature : otherFeaturesTokens) {
            taNames.put(otherFeature, "Cohesion-l Other BERT Token Embedding Features");
            MapShortNames.put(otherFeature, otherFeature);
        }
        taNames.put("btrac", "Recursive Autocorrelation of BERT token probabilities");
        taNames.put("bt_prod_rac", "Recursive Autocorrelation of BERT token probabilities");
        taNames.put("bt_first_rac", "Recursive Autocorrelation of BERT token probabilities");
        taNames.put("bt_mean_rac", "Recursive Autocorrelation of BERT token probabilities");
        taNames.put("bt_min_rac", "Recursive Autocorrelation of BERT token probabilities");
        taNames.put("bt_max_rac", "Recursive Autocorrelation of BERT token probabilities");

        MapShortNames.put("btrac", "all subwords");
        MapShortNames.put("bt_prod_rac", "aggregated via subword product");
        MapShortNames.put("bt_first_rac", "first subwords only");
        MapShortNames.put("bt_mean_rac", "subword average aggregation");
        MapShortNames.put("bt_min_rac", "subword minimum aggregation");
        MapShortNames.put("bt_max_rac", "subword maximum aggregation");

        taNames.put("btradc", "Recursive Auto Distance Correlation of BERT token probabilities");
        taNames.put("bt_prod_radc", "Recursive Auto Distance Correlation of BERT token probabilities");
        taNames.put("bt_first_radc", "Recursive Auto Distance Correlation of BERT token probabilities");
        taNames.put("bt_mean_radc", "Recursive Auto Distance Correlation of BERT token probabilities");
        taNames.put("bt_min_radc", "Recursive Auto Distance Correlation of BERT token probabilities");
        taNames.put("bt_max_radc", "Recursive Auto Distance Correlation of BERT token probabilities");

        MapShortNames.put("btradc", "all subwords");
        MapShortNames.put("bt_prod_radc", "aggregated via subword product");
        MapShortNames.put("bt_first_radc", "first subwords only");
        MapShortNames.put("bt_mean_radc", "subword average aggregation");
        MapShortNames.put("bt_min_radc", "subword minimum aggregation");
        MapShortNames.put("bt_max_radc", "subword maximum aggregation");

        taMapNames.put("properties", taNames);
        LinkedHashMap<String, String> submodels = new LinkedHashMap<>();

        submodels.put("Autocorrelation of BERT token probabilities (all subwords)", "TTLAB Cohesion-l BERT Token Auto Correlation");
        submodels.put("Autocorrelation of BERT token probabilities (aggregated via subword product)", "TTLAB Cohesion-l BERT Token Auto Correlation");
        submodels.put("Autocorrelation of BERT token probabilities (first subwords only)", "TTLAB Cohesion-l BERT Token Auto Correlation");
        submodels.put("Autocorrelation of BERT token probabilities (subword average aggregation)", "TTLAB Cohesion-l BERT Token Auto Correlation");
        submodels.put("Autocorrelation of BERT token probabilities (subword minimum aggregation)", "TTLAB Cohesion-l BERT Token Auto Correlation");
        submodels.put("Autocorrelation of BERT token probabilities (subword maximum aggregation)", "TTLAB Cohesion-l BERT Token Auto Correlation");
        submodels.put("Recursive Autocorrelation of BERT token probabilities", "TTLAB Cohesion-l BERT Token Auto Correlation");

        submodels.put("Auto Distance Correlation of BERT token probabilities (all subwords)", "TTLAB Cohesion-l BERT Token Auto Distance Correlation");
        submodels.put("Auto Distance Correlation of BERT token probabilities (aggregated via subword product)", "TTLAB Cohesion-l BERT Token Auto Distance Correlation");
        submodels.put("Auto Distance Correlation of BERT token probabilities (first subwords only)", "TTLAB Cohesion-l BERT Token Auto Distance Correlation");
        submodels.put("Auto Distance Correlation of BERT token probabilities (subword average aggregation)", "TTLAB Cohesion-l BERT Token Auto Distance Correlation");
        submodels.put("Auto Distance Correlation of BERT token probabilities (subword minimum aggregation)", "TTLAB Cohesion-l BERT Token Auto Distance Correlation");
        submodels.put("Auto Distance Correlation of BERT token probabilities (subword maximum aggregation)", "TTLAB Cohesion-l BERT Token Auto Distance Correlation");
        submodels.put("Recursive Auto Distance Correlation of BERT token probabilities", "TTLAB Cohesion-l BERT Token Auto Distance Correlation");

        submodels.put("Autocorrelation of BERT sentence probabilities", "TTLAB Cohesion-s BERT Sentence Auto Correlation & Auto-Distance-Correlation");
        submodels.put("Auto Distance Correlation of BERT sentence probabilities", "TTLAB Cohesion-s BERT Sentence Auto Correlation & Auto-Distance-Correlation");

        submodels.put("Syntactic Mean Aggregated Features", "TTLAB Syntactic Features");
        submodels.put("Syntactic Entropy Aggregated Features", "TTLAB Syntactic Features");
        submodels.put("Syntactic Gini Coefficient Aggregated Features", "TTLAB Syntactic Features");
        submodels.put("Syntactic Recursive Auto Correlation Aggregated Features", "TTLAB Syntactic Features");
        submodels.put("Syntactic Auto Dynamic Time Warping Aggregated Features", "TTLAB Syntactic Features");

        submodels.put("Cohesion-l Other BERT Token Embedding Features", "TTLAB Cohesion BERT Others");
        submodels.put("Cohesion-s Other BERT Sentence Embedding Features", "TTLAB Cohesion BERT Others");

        taMapNames.put("submodels", submodels);
        taMapNames.put("labels", MapShortNames);
        return taMapNames;
    }
}
