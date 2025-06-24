package org.texttechnologylab.modules;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class TTLabScorerInfo {

    public LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>> getTaInputMap() {
        LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>> taInputMap = new LinkedHashMap<>();
        LinkedHashMap<String, LinkedHashMap<String, String>> taInput = new LinkedHashMap<>();

        LinkedHashMap<String, String> AllScoresbtac = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScoresbt_prod_ac = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScoresbt_bt_first_ac = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScoresbt_bt_mean_ac = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScoresbt_bt_min_ac = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScoresbt_bt_max_ac = new LinkedHashMap<>();
        LinkedHashMap<String, String> AllScoresbt_btrac = new LinkedHashMap<>();

        for (int i = 0; i < 110; i++) {
            String key = "lag" + i;
            AllScoresbtac.put(key, "btac" + i);
            AllScoresbt_prod_ac.put(key, "bt_prod_ac" + i);
            AllScoresbt_bt_first_ac.put(key, "bt_bt_first_ac" + i);
            AllScoresbt_bt_mean_ac.put(key, "bt_bt_mean_ac" + i);
            AllScoresbt_bt_min_ac.put(key, "bt_bt_min_ac" + i);
            AllScoresbt_bt_max_ac.put(key, "bt_bt_max_ac" + i);
        }

        AllScoresbt_btrac.put("all subwords", "btrac");
        AllScoresbt_btrac.put("aggregated via subword product", "bt_prod_rac");
        AllScoresbt_btrac.put("first subwords only", "bt_bt_first_rac");
        AllScoresbt_btrac.put("subword average aggregation", "bt_bt_mean_rac");
        AllScoresbt_btrac.put("subword minimum aggregation", "bt_bt_min_rac");
        AllScoresbt_btrac.put("subword maximum aggregation", "bt_bt_max_rac");

        taInput.put("Autocorrelation of BERT token probabilities (all subwords)", AllScoresbtac);
        taInput.put("Autocorrelation of BERT token probabilities (aggregated via subword product)", AllScoresbt_prod_ac);
        taInput.put("Autocorrelation of BERT token probabilities  (first subwords only)", AllScoresbt_bt_first_ac);
        taInput.put("Autocorrelation of BERT token probabilities (subword average aggregation)", AllScoresbt_bt_mean_ac);
        taInput.put("Autocorrelation of BERT token probabilities (subword minimum aggregation)", AllScoresbt_bt_min_ac);
        taInput.put("Autocorrelation of BERT token probabilities (subword maximum aggregation)", AllScoresbt_bt_max_ac);
        taInput.put("Recursive Autocorrelation of BERT token probabilities", AllScoresbt_btrac);

        taInputMap.put("TTLAB Cohesion-l BERT Token Auto Correlation", taInput);
        return taInputMap;
    }
}
