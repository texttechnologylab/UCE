package org.texttechnologylab.uce.analysis;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.uce.analysis.modules.*;
import org.texttechnologylab.uce.analysis.typeClasses.TextClass;




import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
// Added imports (save .jcas, HTTP import, logging)
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.Files;
//import java.io.OutputStream;
import java.io.InputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import java.util.*;


public class RunDUUIPipeline {
    private static final AnalysisCache analysisCache = new AnalysisCache();
    private static final ThreadLocal<String> lastAnalysisIdTL = new ThreadLocal<>();

    public static AnalysisSession getCachedSession(String analysisId) { return analysisCache.get(analysisId); }

    private static String getCurrentUserId() {
        // TODO: replace with your auth/session identity
        return "user-unknown";
    }

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

            System.out.println("[CAS] Created secondary JCas for special models (fact/coherence/stance/LLM)");

        }
        // run pipeline
        DUUIComposer composer = pipeline.setComposer(modelInfosMap);
        JCas result = pipeline.runPipeline(cas, composer);
        System.out.println("[CAS] Final result JCas created via pipeline.runPipeline(cas, composer)");
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
        String analysisId = UUID.randomUUID().toString();
        String userId = getCurrentUserId();
        String title = "Analysis " + Instant.now();

        byte[] xmiBytes = toXmiBytes(result);
        AnalysisSession session = new AnalysisSession(
                analysisId, userId, title, /*externalId*/ null,
                result, /*xmiBytes*/ xmiBytes
        );
        analysisCache.put(session);
        lastAnalysisIdTL.set(analysisId);
        System.out.println("[CACHE] Added analysisId=" + analysisId + " (stored in memory; TTL=45min)");
        return duuiInformation;
    }

    public AnalysisResponse getModelResourcesWithHandle(List<String> modelGroups, String inputText, String claim,
                                                        String coherenceText, String stanceText, String systemPrompt) throws Exception {
        DUUIInformation info = getModelResources(modelGroups, inputText, claim, coherenceText, stanceText, systemPrompt);
        String id = lastAnalysisIdTL.get();
        return new AnalysisResponse(id, info);
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
    public static final class AnalysisResponse {
        public final String analysisId;
        public final DUUIInformation duuiInformation;

        public AnalysisResponse(String analysisId, DUUIInformation duuiInformation) {
            this.analysisId = analysisId;
            this.duuiInformation = duuiInformation;
        }
    }


    //AnalysisSession
    public static final class AnalysisSession {
        public final String analysisId;
        public final String userId;
        public final long createdAtMillis;
        public final String title;
        public final String externalId;
        public final JCas jcas;
        public final byte[] xmiBytes;

        public AnalysisSession(String analysisId, String userId, String title, String externalId,
                               JCas jcas, byte[] xmiBytes) {
            this.analysisId = analysisId;
            this.userId = userId;
            this.title = title;
            this.externalId = externalId;
            this.createdAtMillis = System.currentTimeMillis();
            this.jcas = jcas;
            this.xmiBytes = xmiBytes;
        }
    }


    //  AnalysisCache
    public static final class AnalysisCache {
        private final Map<String, AnalysisSession> map = new ConcurrentHashMap<>();
        private final long ttlMillis = 45 * 60 * 1000L; // 45 minutes

        public void put(AnalysisSession s) { map.put(s.analysisId, s); }

        public AnalysisSession get(String id) { // Retrieve a session from the cache
            AnalysisSession s = map.get(id);
            if (s == null) return null;

            if (System.currentTimeMillis() - s.createdAtMillis > ttlMillis) { // If this session is older than 45 minutes -> expire it
                map.remove(id);
                return null;
            }
            return s;
        }

        public void remove(String id) { map.remove(id); } //Manually remove a session by ID


        public void cleanupExpired() { //  cleanup all expired sessions
            long now = System.currentTimeMillis();
            for (var entry : map.entrySet()) {
                AnalysisSession s = entry.getValue();
                if (now - s.createdAtMillis > ttlMillis) {
                    map.remove(entry.getKey());
                    System.out.println("[CRON] Removed expired session: " + s.analysisId);
                }
            }
        }
    }
    private static final java.util.concurrent.ScheduledExecutorService scheduler = //Cron job for automatic cleanup every 5 minutes
            java.util.concurrent.Executors.newScheduledThreadPool(1);

    static {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                analysisCache.cleanupExpired();
            } catch (Exception e) {
                System.err.println("[CACHE] Cache cleanup failed: " + e.getMessage());
            }
        }, 5, 5, java.util.concurrent.TimeUnit.MINUTES);

        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("[CACHE] Running cache cleanup task...");
            analysisCache.cleanupExpired();  // your cleanup method
        }, 1, 5, TimeUnit.MINUTES);


    }


    private static byte[] toXmiBytes(org.apache.uima.jcas.JCas jcas) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        org.apache.uima.cas.impl.XmiCasSerializer ser =
                new org.apache.uima.cas.impl.XmiCasSerializer(jcas.getTypeSystem());
        org.apache.uima.util.XMLSerializer xmlSer =
                new org.apache.uima.util.XMLSerializer(bos, true);
        xmlSer.setOutputProperty(javax.xml.transform.OutputKeys.VERSION, "1.1");
        ser.serialize(jcas.getCas(), xmlSer.getContentHandler());
        return bos.toByteArray();
    }


    // When we send CAS to the importer via HTTP, we want to capture the response.
    // This small class acts like a container for the HTTP response details
    private static class HttpResult {
        final int status;
        final String body;
        final String locationHeader;
        HttpResult(int status, String body, String locationHeader) {
            this.status = status; this.body = body; this.locationHeader = locationHeader;
        }
    }


    // Send CAS via HTTP
    private static HttpResult postMultipart(String urlStr,
                                            Map<String, String> fields,
                                            String fileField, String filename,
                                            String fileContentType, byte[] fileBytes) throws Exception {
        String boundary = "----JAVA-" + UUID.randomUUID(); //Generate a boundary string to separate parts in multipart body
        URL url = new URL(urlStr); //Open HTTP connection to the importer endpoint
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) { //Write request body
            // text fields
            for (var e : fields.entrySet()) {
                out.writeBytes("--" + boundary + "\r\n");
                out.writeBytes("Content-Disposition: form-data; name=\"" + e.getKey() + "\"\r\n\r\n");
                out.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                out.writeBytes("\r\n");
            }
            // file field
            out.writeBytes("--" + boundary + "\r\n");
            out.writeBytes("Content-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + filename + "\"\r\n");
            out.writeBytes("Content-Type: " + fileContentType + "\r\n\r\n");
            out.write(fileBytes);
            out.writeBytes("\r\n");
            out.writeBytes("--" + boundary + "--\r\n");
            out.flush();
        }

        int status = conn.getResponseCode(); //Read the HTTP response from the importer
        String location = conn.getHeaderField("Location");
        String body;

        try (InputStream in = (status >= 200 && status < 400) ? conn.getInputStream() : conn.getErrorStream()) {
            body = (in != null) ? new String(in.readAllBytes(), StandardCharsets.UTF_8) : "";
        }
        conn.disconnect();
        return new HttpResult(status, body, location);
    }

    public static HttpResult sendToImporterViaHttp(String importUrl, //Send cached CAS to importer
                                                   String analysisId,
                                                   long corpusId,
                                                   String documentId,
                                                   String casView) throws Exception {
        AnalysisSession s = getCachedSession(analysisId);
        if (s == null) throw new IllegalArgumentException("No cached session for id: " + analysisId);

        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(); //  Convert JCas -> XMI bytes
        org.apache.uima.cas.impl.XmiCasSerializer ser =
                new org.apache.uima.cas.impl.XmiCasSerializer(s.jcas.getTypeSystem());
        org.apache.uima.util.XMLSerializer xmlSer =
                new org.apache.uima.util.XMLSerializer(bos, /*prettyPrint*/ true);
        xmlSer.setOutputProperty(javax.xml.transform.OutputKeys.VERSION, "1.1");
        ser.serialize(s.jcas.getCas(), xmlSer.getContentHandler());
        byte[] casBytes = bos.toByteArray();

        Map<String, String> fields = new LinkedHashMap<>(); //  Form-data fields
        fields.put("analysisId", analysisId);
        fields.put("corpusId", Long.toString(corpusId));
        if (documentId != null && !documentId.isBlank()) fields.put("documentId", documentId);
        if (casView != null && !casView.isBlank()) fields.put("casView", casView);

        String corpusConfigJson = System.getenv("UCE_CORPUS_CONFIG_JSON"); // Include corpusConfig
        if (corpusConfigJson == null || corpusConfigJson.isBlank()) {
            String cfgPath = System.getenv("UCE_CORPUS_CONFIG_PATH");
            if (cfgPath != null && !cfgPath.isBlank()) {
                corpusConfigJson = java.nio.file.Files.readString(
                        java.nio.file.Path.of(cfgPath),
                        java.nio.charset.StandardCharsets.UTF_8
                );
            }
        }
        if (corpusConfigJson != null && !corpusConfigJson.isBlank()) {
            fields.put("corpusConfig", corpusConfigJson);
        }

        // Send multipart as XMI
        String filename = "cas_" + analysisId + ".xmi";
        System.out.println("[IMPORT][HTTP] POST " + importUrl
                + " corpusId=" + corpusId + " analysisId=" + analysisId
                + " documentId=" + documentId + " casView=" + casView
                + " file=" + filename + " (" + casBytes.length + " bytes)");

        HttpResult res = postMultipart(
                importUrl,
                fields,
                "file",
                filename,
                "application/xml",
                casBytes
        );
        System.out.println("[IMPORT][HTTP] status=" + res.status
                + (res.locationHeader != null ? " Location=" + res.locationHeader : "")
                + (res.body != null && !res.body.isBlank() ? " body=" + res.body : ""));
        return res;
    }


}
