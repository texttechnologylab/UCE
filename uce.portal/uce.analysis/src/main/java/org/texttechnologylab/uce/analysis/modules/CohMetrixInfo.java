package org.texttechnologylab.uce.analysis.modules;
import java.util.LinkedHashMap;

public class CohMetrixInfo {
    String[] descriptiveLabels = {"DESPC", "DESSC", "DESWC", "DESPL", "DESPLd", "DESSL", "DESSLd", "DESWLsy", "DESWLsyd", "DESWLlt", "DESWLltd"};
    String[] descriptiveNames = {"Paragraph count, number of paragraphs", "Sentence count, number of sentences", "Word count, number of words", "Paragraph length, number of sentences, mean", "Paragraph length, number of sentences, standard deviation", "Sentence length, number of words, mean", "Sentence length, number of words, standard deviation", "Word length, number of syllables, mean", "Word length, number of syllables, standard deviation", "Word length, number of letters, mean", "Word length, number of letters, standard deviation"};
    String[] easabilityLabels = {"PCNARz", "PCNARp", "PCSYNz", "PCSYNp", "PCCNCz", "PCCNCp", "PCREFz", "PCREFp", "PCDCz", "PCDCp", "PCVERBz", "PCVERBp", "PCCONNz", "PCCONNp","PCTEMPz", "PCTEMPp"};
    String[] easabilityNames = {"Text Easability PC Narrativity, z score", "Text Easability PC Narrativity, percentile", "Text Easability PC Syntactic Simplicity, z score", "Text Easability PC Syntactic Simplicity, percentile", "Text Easability PC Word concreteness, z score", "Text Easability PC Word concreteness, percentile", "Text Easability PC Referential Cohesion, z score", "Text Easability PC Referential Cohesion, percentile", "Text Easability PC Deep cohesion, z score", "Text Easability PC Deep cohesion, percentile", "Text Easability PC Verb Cohesion, z score", "Text Easability PC Verb Cohesion, percentile", "Text Easability PC Connectivity, z score", "Text Easability PC Connectivity, percentile", "Text Easability PC Temporality, z score", "Text Easability PC Temporality, percentile"};
    String[] refCohesionLabels = {"CRFNO1", "CRFAO1", "CRFSO1", "CRFNOa", "CRFAOa", "CRFSOa", "CRFCWO1", "CRFCWO1d", "CRFCWOa", "CRFCWOad"};
    String[] refCohesionNames = {"Noun overlap, adjacent sentences, binary, mean", "Argument overlap, adjacent sentences, binary, mean", "Stem overlap, adjacent sentences, binary, mean", "Noun overlap, all sentences, binary, mean", "Argument overlap, all sentences, binary, mean", "Stem overlap, all sentences, binary, mean", "Content word overlap, adjacent sentences, proportional, mean", "Content word overlap, adjacent sentences, proportional, standard deviation", "Content word overlap, all sentences, proportional, mean", "Content word overlap, all sentences, proportional, standard deviation"};
    String[] LSALabels = {"LSASS1", "LSASS1d", "LSASSp", "LSASSpd", "LSAPP1", "LSAPP1d", "LSAGN", "LSAGNd"};
    String[] LSANames = {"LSA overlap, adjacent sentences, mean", "LSA overlap, adjacent sentences, standard deviation", "LSA overlap, all sentences in paragraph, mean", "LSA overlap, all sentences in paragraph, standard deviation", "LSA overlap, adjacent paragraphs, mean", "LSA overlap, adjacent paragraphs, standard deviation", "LSA given/new, sentences, mean", "LSA given/new, sentences, standard deviation"};
    String[] lexicalDiversityLabels = {"LDTTRc", "LDTTRa", "LDMTLDa", "LDVOCDa"};
    String[] lexicalDiversityNames = {"Lexical diversity, type-token ratio, content word lemmas", "Lexical diversity, type-token ratio, all words", "Lexical diversity, MTLD, all words", "Lexical diversity, VOCD, all words"};
    String[] connectivesLabels = {"CNCAll", "CNCCaus", "CNCLogic", "CNCADC", "CNCTemp", "CNCTempx", "CNCAdd", "CNCPos", "CNCNeg"};
    String[] connectivesNames = {"All connectives incidence", "Causal connectives incidence", "Logical connectives incidence", "Adversative and contrastive connectives incidence", "Temporal connectives incidence", "Expanded temporal connectives incidence", "Additive connectives incidence", "Positive connectives incidence", "Negative connectives incidence"};
    String[] situationModelLabels = {"SMCAUSv", "SMCAUSvp", "SMINTEp", "SMCAUSr", "SMINTEr", "SMCAUSlsa", "SMCAUSwn", "SMTEMP"};
    String[] situationModelNames = {"Causal verb incidence", "Causal verbs and causal particles incidence", "Intentional verbs incidence", "Ratio of causal particles to causal verbs", "Ratio of intentional particles to intentional verbs", "LSA verb overlap", "WordNet verb overlap", "Temporal cohesion, tense and aspect repetition, mean"};
    String[] syntacticComplexityLabels = {"SYNLE", "SYNNP", "SYNMEDpos", "SYNMEDwrd", "SYNMEDlem", "SYNSTRUTa", "SYNSTRUTt"};
    String[] syntacticComplexityNames = {"Left embeddedness, words before main verb, mean", "Number of modifiers per noun phrase, mean", "Minimal Edit Distance, part of speech", "Minimal Edit Distance, all words", "Minimal Edit Distance, lemmas", "Sentence syntax similarity, adjacent sentences, mean", "Sentence syntax similarity, all combinations, across paragraphs, mean"};
    String[] syntacticPatternLabels = {"DRNP", "DRVP", "DRAP", "DRPP", "DRPVAL", "DRNEG", "DRGERUND", "DRINF"};
    String[] syntacticPatternNames = {"Noun phrase density, incidence", "Verb phrase density, incidence", "Adjective phrase density, incidence", "Preposition phrase density, incidence", "Agentless passive voice density, incidence", "Negation density, incidence", "Gerund density, incidence", "Infinitive density, incidence"};
    String[] wordInformationLabels = {"WRDNOUN", "WRDVERB", "WRDADJ", "WRDADV", "WRDPRO", "WRDPRP1s", "WRDPRP1p", "WRDPRP2", "WRDPRP3s", "WRDPRP3p", "WRDFRQc", "WRDFRQc", "WRDFRQmc", "WRDAOAc", "WRDFAMc", "WRDCNCc", "WRDIMGc", "WRDMEAc", "WRDPOLc", "WRDHYPn", "WRDHYPv", "WRDHYPnv"};
    String[] wordInformationNames = {"Noun incidence", "Verb incidence", "Adjective incidence", "Adverb incidence", "Pronoun incidence", "First-person singular pronoun incidence", "First-person plural pronoun incidence", "Second-person pronoun incidence", "Third-person singular pronoun incidence", "Third-person plural pronoun incidence", "Database word frequency for content words, mean", "Database Log frequency for all words, mean", "Database Log minimum frequency for content words, mean", "Age of acquisition for content words, mean", "Familiarity for content words, mean", "Concreteness for content words, mean", "Imageability for content words, mean", "Meaningfulness for content words, mean", "Polysemy for content words, mean", "Hypernymy for nouns, mean", "Hypernymy for verbs, mean", "Hypernymy for nouns and verbs, mean"};
    String[] readabilityLabels = {"RDFRE", "RDFKGL", "RDL2_synstruta", "RDL2_synstrutt"};
    String[] readabilityNames = {"Flesch Reading Ease", "Flesch-Kincaid Grade Level", "Coh-Metrix L2 Readability", "Coh-Metrix L2 Readability"};

    public ModelInfo getModelInfo() {
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setName("CohMetrix");
        modelInfo.setKey("cohmetrix");
        modelInfo.setUrl("http://readability-cohmetrix.service.component.duui.texttechnologylab.org/");
        modelInfo.setGithub("https://github.com/mevbagci/duui-uima/tree/main/duui-cohmetrix");
        modelInfo.setHuggingface("");
        modelInfo.setPaper("");
        modelInfo.setMap("cohmetrix");
        modelInfo.setVariant("cohmetrix");
        modelInfo.setMainTool("CohMetrix");
        modelInfo.setModelType("cohmetrix");
        return modelInfo;
    }

    public LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>> getCohMetrixMap() {
        LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>> cohmetrixMap = new LinkedHashMap<>();

        LinkedHashMap<String, LinkedHashMap<String, String>> cohmetrixDescriptive = new LinkedHashMap<>();
        for (int i = 0; i < descriptiveLabels.length; i++) {
            String descriptiveLabel = descriptiveLabels[i];
            String descriptiveName = descriptiveNames[i];
            LinkedHashMap<String, String> cohmetrixLabelDescription = new LinkedHashMap<>();
            cohmetrixLabelDescription.put("label", descriptiveLabel);
            cohmetrixLabelDescription.put("description", descriptiveName);
            cohmetrixLabelDescription.put("active", "true");
            cohmetrixLabelDescription.put("model", "Descriptive");
            cohmetrixDescriptive.put(descriptiveLabel, cohmetrixLabelDescription);
        }
        cohmetrixMap.put("Descriptive", cohmetrixDescriptive);

        LinkedHashMap<String, LinkedHashMap<String, String>> cohmetrixEasability = new LinkedHashMap<>();
        for (int i = 0; i < easabilityLabels.length; i++) {
            String easabilityLabel = easabilityLabels[i];
            String easabilityName = easabilityNames[i];
            LinkedHashMap<String, String> cohmetrixLabelDescription = new LinkedHashMap<>();
            cohmetrixLabelDescription.put("label", easabilityLabel);
            cohmetrixLabelDescription.put("description", easabilityName);
            cohmetrixLabelDescription.put("active", "false");
            cohmetrixLabelDescription.put("model", "Text Easability Principal Component Scores");
            cohmetrixEasability.put(easabilityLabel, cohmetrixLabelDescription);
        }
        cohmetrixMap.put("Text Easability Principal Component Scores", cohmetrixEasability);

        LinkedHashMap<String, LinkedHashMap<String, String>> cohmetrixRefCohesion = new LinkedHashMap<>();
        for (int i = 0; i < refCohesionLabels.length; i++) {
            String refCohesionLabel = refCohesionLabels[i];
            String refCohesionName = refCohesionNames[i];
            LinkedHashMap<String, String> cohmetrixLabelDescription = new LinkedHashMap<>();
            cohmetrixLabelDescription.put("label", refCohesionLabel);
            cohmetrixLabelDescription.put("description", refCohesionName);
            cohmetrixLabelDescription.put("active", "True");
            cohmetrixLabelDescription.put("model", "Referential Cohesion");
            cohmetrixRefCohesion.put(refCohesionLabel, cohmetrixLabelDescription);
        }
        cohmetrixMap.put("Referential Cohesion", cohmetrixRefCohesion);

        LinkedHashMap<String, LinkedHashMap<String, String>> cohmetrixLSA = new LinkedHashMap<>();
        for (int i = 0; i < LSALabels.length; i++) {
            String LSALabel = LSALabels[i];
            String LSAName = LSANames[i];
            LinkedHashMap<String, String> cohmetrixLabelDescription = new LinkedHashMap<>();
            cohmetrixLabelDescription.put("label", LSALabel);
            cohmetrixLabelDescription.put("description", LSAName);
            cohmetrixLabelDescription.put("active", "True");
            cohmetrixLabelDescription.put("model", "Latent Semantic Analysis");
            cohmetrixLSA.put(LSALabel, cohmetrixLabelDescription);
        }
        cohmetrixMap.put("Latent Semantic Analysis", cohmetrixLSA);

        LinkedHashMap<String, LinkedHashMap<String, String>> cohmetrixLexicalDiversity = new LinkedHashMap<>();
        for (int i = 0; i < lexicalDiversityLabels.length; i++) {
            String lexicalDiversityLabel = lexicalDiversityLabels[i];
            String lexicalDiversityName = lexicalDiversityNames[i];
            LinkedHashMap<String, String> cohmetrixLabelDescription = new LinkedHashMap<>();
            cohmetrixLabelDescription.put("label", lexicalDiversityLabel);
            cohmetrixLabelDescription.put("description", lexicalDiversityName);
            cohmetrixLabelDescription.put("active", "True");
            cohmetrixLabelDescription.put("model", "Lexical Diversity");
            cohmetrixLexicalDiversity.put(lexicalDiversityLabel, cohmetrixLabelDescription);
        }
        cohmetrixMap.put("Lexical Diversity", cohmetrixLexicalDiversity);

        LinkedHashMap<String, LinkedHashMap<String, String>> cohmetrixConnectives = new LinkedHashMap<>();
        for (int i = 0; i < connectivesLabels.length; i++) {
            String connectivesLabel = connectivesLabels[i];
            String connectivesName = connectivesNames[i];
            LinkedHashMap<String, String> cohmetrixLabelDescription = new LinkedHashMap<>();
            cohmetrixLabelDescription.put("label", connectivesLabel);
            cohmetrixLabelDescription.put("description", connectivesName);
            cohmetrixLabelDescription.put("active", "True");
            cohmetrixLabelDescription.put("model", "Connectives");
            cohmetrixConnectives.put(connectivesLabel, cohmetrixLabelDescription);
        }
        cohmetrixMap.put("Connectives", cohmetrixConnectives);

        LinkedHashMap<String, LinkedHashMap<String, String>> cohmetrixSituationModel = new LinkedHashMap<>();
        for (int i = 0; i < situationModelLabels.length; i++) {
            String situationModelLabel = situationModelLabels[i];
            String situationModelName = situationModelNames[i];
            LinkedHashMap<String, String> cohmetrixLabelDescription = new LinkedHashMap<>();
            cohmetrixLabelDescription.put("label", situationModelLabel);
            cohmetrixLabelDescription.put("description", situationModelName);
            cohmetrixLabelDescription.put("active", "True");
            cohmetrixLabelDescription.put("model", "Situation Model");
            cohmetrixSituationModel.put(situationModelLabel, cohmetrixLabelDescription);
        }
        cohmetrixMap.put("Situation Model", cohmetrixSituationModel);

        LinkedHashMap<String, LinkedHashMap<String, String>> cohmetrixSyntacticComplexity = new LinkedHashMap<>();
        for (int i = 0; i < syntacticComplexityLabels.length; i++) {
            String syntacticComplexityLabel = syntacticComplexityLabels[i];
            String syntacticComplexityName = syntacticComplexityNames[i];
            LinkedHashMap<String, String> cohmetrixLabelDescription = new LinkedHashMap<>();
            cohmetrixLabelDescription.put("label", syntacticComplexityLabel);
            cohmetrixLabelDescription.put("description", syntacticComplexityName);
            cohmetrixLabelDescription.put("active", "True");
            cohmetrixLabelDescription.put("model", "Syntactic Complexity");
            cohmetrixSyntacticComplexity.put(syntacticComplexityLabel, cohmetrixLabelDescription);
        }
        cohmetrixMap.put("Syntactic Complexity", cohmetrixSyntacticComplexity);

        LinkedHashMap<String, LinkedHashMap<String, String>> cohmetrixSyntacticPatterns = new LinkedHashMap<>();
        for (int i = 0; i < syntacticPatternLabels.length; i++) {
            String syntacticPatternLabel = syntacticPatternLabels[i];
            String syntacticPatternName = syntacticPatternNames[i];
            LinkedHashMap<String, String> cohmetrixLabelDescription = new LinkedHashMap<>();
            cohmetrixLabelDescription.put("label", syntacticPatternLabel);
            cohmetrixLabelDescription.put("description", syntacticPatternName);
            cohmetrixLabelDescription.put("active", "True");
            cohmetrixLabelDescription.put("model", "Syntactic Patterns Density");
            cohmetrixSyntacticPatterns.put(syntacticPatternLabel, cohmetrixLabelDescription);
        }
        cohmetrixMap.put("Syntactic Patterns Density", cohmetrixSyntacticPatterns);

        LinkedHashMap<String, LinkedHashMap<String, String>> cohmetrixWordInformation = new LinkedHashMap<>();
        for (int i = 0; i < wordInformationLabels.length; i++) {
            String wordInformationLabel = wordInformationLabels[i];
            String wordInformationName = wordInformationNames[i];
            LinkedHashMap<String, String> cohmetrixLabelDescription = new LinkedHashMap<>();
            cohmetrixLabelDescription.put("label", wordInformationLabel);
            cohmetrixLabelDescription.put("description", wordInformationName);
            cohmetrixLabelDescription.put("active", "True");
            cohmetrixLabelDescription.put("model", "Word Information");
            cohmetrixWordInformation.put(wordInformationLabel, cohmetrixLabelDescription);
        }
        cohmetrixMap.put("Word Information", cohmetrixWordInformation);

        LinkedHashMap<String, LinkedHashMap<String, String>> cohmetrixReadability = new LinkedHashMap<>();
        for (int i = 0; i < readabilityLabels.length; i++) {
            String readabilityLabel = readabilityLabels[i];
            String readabilityName = readabilityNames[i];
            LinkedHashMap<String, String> cohmetrixLabelDescription = new LinkedHashMap<>();
            cohmetrixLabelDescription.put("label", readabilityLabel);
            cohmetrixLabelDescription.put("description", readabilityName);
            cohmetrixLabelDescription.put("active", "True");
            cohmetrixLabelDescription.put("model", "Readability");
            cohmetrixReadability.put(readabilityLabel, cohmetrixLabelDescription);
        }
        cohmetrixMap.put("Readability", cohmetrixReadability);

        return cohmetrixMap;
    }


    public LinkedHashMap<String, LinkedHashMap<String, String>> getCohMetrixMapInfo() {
        LinkedHashMap<String, LinkedHashMap<String, String>> cohmetrixMapInfo = new LinkedHashMap<>();

        LinkedHashMap<String, String> descriptiveInfo = new LinkedHashMap<>();
        LinkedHashMap<String, String> modelMap = new LinkedHashMap<>();
        for (int i = 0; i < descriptiveLabels.length; i++) {
            String descriptiveLabel = descriptiveLabels[i];
            String descriptiveName = descriptiveNames[i];
            descriptiveInfo.put(descriptiveLabel, descriptiveName);
            modelMap.put(descriptiveLabel, "Descriptive");
        }

        for (int i = 0; i < easabilityLabels.length; i++) {
            String easabilityLabel = easabilityLabels[i];
            String easabilityName = easabilityNames[i];
            descriptiveInfo.put(easabilityLabel, easabilityName);
            modelMap.put(easabilityLabel, "Text Easability Principal Component Scores");
        }

        for (int i = 0; i < refCohesionLabels.length; i++) {
            String refCohesionLabel = refCohesionLabels[i];
            String refCohesionName = refCohesionNames[i];
            descriptiveInfo.put(refCohesionLabel, refCohesionName);
            modelMap.put(refCohesionLabel, "Referential Cohesion");
        }

        for (int i = 0; i < LSALabels.length; i++) {
            String LSALabel = LSALabels[i];
            String LSAName = LSANames[i];
            descriptiveInfo.put(LSALabel, LSAName);
            modelMap.put(LSALabel, "Latent Semantic Analysis");
        }

        for (int i = 0; i < lexicalDiversityLabels.length; i++) {
            String lexicalDiversityLabel = lexicalDiversityLabels[i];
            String lexicalDiversityName = lexicalDiversityNames[i];
            descriptiveInfo.put(lexicalDiversityLabel, lexicalDiversityName);
            modelMap.put(lexicalDiversityLabel, "Lexical Diversity");
        }

        for (int i = 0; i < connectivesLabels.length; i++) {
            String connectivesLabel = connectivesLabels[i];
            String connectivesName = connectivesNames[i];
            descriptiveInfo.put(connectivesLabel, connectivesName);
            modelMap.put(connectivesLabel, "Connectives");
        }

        for (int i = 0; i < situationModelLabels.length; i++) {
            String situationModelLabel = situationModelLabels[i];
            String situationModelName = situationModelNames[i];
            descriptiveInfo.put(situationModelLabel, situationModelName);
            modelMap.put(situationModelLabel, "Situation Model");
        }

        for (int i = 0; i < syntacticComplexityLabels.length; i++) {
            String syntacticComplexityLabel = syntacticComplexityLabels[i];
            String syntacticComplexityName = syntacticComplexityNames[i];
            descriptiveInfo.put(syntacticComplexityLabel, syntacticComplexityName);
            modelMap.put(syntacticComplexityLabel, "Syntactic Complexity");
        }

        for (int i = 0; i < syntacticPatternLabels.length; i++) {
            String syntacticPatternLabel = syntacticPatternLabels[i];
            String syntacticPatternName = syntacticPatternNames[i];
            descriptiveInfo.put(syntacticPatternLabel, syntacticPatternName);
            modelMap.put(syntacticPatternLabel, "Syntactic Patterns Density");
        }

        for (int i = 0; i < wordInformationLabels.length; i++) {
            String wordInformationLabel = wordInformationLabels[i];
            String wordInformationName = wordInformationNames[i];
            descriptiveInfo.put(wordInformationLabel, wordInformationName);
            modelMap.put(wordInformationLabel, "Word Information");
        }

        for (int i = 0; i < readabilityLabels.length; i++) {
            String readabilityLabel = readabilityLabels[i];
            String readabilityName = readabilityNames[i];
            descriptiveInfo.put(readabilityLabel, readabilityName);
            modelMap.put(readabilityLabel, "Readability");
        }

        cohmetrixMapInfo.put("Description", descriptiveInfo);
        cohmetrixMapInfo.put("Models", modelMap);


        return cohmetrixMapInfo;
    }


}
