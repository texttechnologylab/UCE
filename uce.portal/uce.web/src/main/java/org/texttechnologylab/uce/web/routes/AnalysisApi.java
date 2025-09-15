package org.texttechnologylab.uce.web.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.uce.analysis.History;
import org.texttechnologylab.uce.analysis.RunDUUIPipeline;
import org.texttechnologylab.uce.analysis.modules.DUUIInformation;
import org.texttechnologylab.uce.common.annotations.auth.Authentication;
import org.texttechnologylab.uce.common.models.dto.AnalysisRequestDto;
import org.texttechnologylab.uce.common.models.dto.HistoryRequestDto;

import java.util.HashMap;
import java.util.List;


public class AnalysisApi implements UceApi {
    private static final Logger logger = LogManager.getLogger(AnalysisApi.class);
    private ApplicationContext context = null;
    private Configuration freemarkerConfig;
    private int counter = 0;
    History history = new History();


    public AnalysisApi(ApplicationContext context, Configuration freemarkerConfig, int counter) {
        this.context = context;
        this.freemarkerConfig = freemarkerConfig;
        this.counter = counter;
    }

    @Authentication(required = Authentication.Requirement.LOGGED_IN,
            route = Authentication.RouteTypes.POST,
            path = "/api/analysis/runPipeline"
    )
    public void runPipeline(Context ctx) {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        try {
            var requestDto = gson.fromJson(ctx.body(), AnalysisRequestDto.class);

            var selectedModels = requestDto.getSelectedModels();
            var inputText = requestDto.getInputText();
            var inputClaim = requestDto.getInputClaim();
            var inputCoherence = requestDto.getInputCoherence();
            var inputStance = requestDto.getInputStance();
            var inputLLM = requestDto.getInputLLM();

            model.put("inputText", inputText);
            model.put("selectedModels", selectedModels);
            model.put("inputClaim", inputClaim);
            model.put("inputCoherence", inputCoherence);
            model.put("inputStance", inputStance);
            model.put("inputLLM", inputLLM);

            RunDUUIPipeline pipeline = new RunDUUIPipeline();
            RunDUUIPipeline.AnalysisResponse resp =
                    pipeline.getModelResourcesWithHandle(selectedModels, inputText, inputClaim,
                            inputCoherence, inputStance, inputLLM);
            DUUIInformation DataRequest = pipeline.getModelResources(selectedModels, inputText, inputClaim, inputCoherence, inputStance, inputLLM);
            model.put("DUUI", DataRequest);
            model.put("SuccessRequest", true);
            model.put("modelGroups", DataRequest.getModelGroups());
            model.put("analysisId", resp.analysisId);
            // set history

            history.addDuuiInformation(String.valueOf(counter), DataRequest);
            history.setModelGroupHashMap(String.valueOf(counter), DataRequest.getModelGroups());
            history.addInputText(String.valueOf(counter), inputText);
            history.addSelectedModels(String.valueOf(counter), selectedModels);
            history.addInputClaim(String.valueOf(counter), inputClaim);
            history.addInputCoherence(String.valueOf(counter), inputCoherence);
            history.addInputStance(String.valueOf(counter), inputStance);
            history.addInputLLM(String.valueOf(counter), inputLLM);
            counter++;

            ctx.render("wiki/analysisResultFragment.ftl", model);

        } catch (Exception ex) {
            logger.error("Error running analysis pipeline with request body: " + ctx.body(), ex);
            ctx.status(500);
            ctx.render("defaultError.ftl");
        }
    }

    @Authentication(required = Authentication.Requirement.LOGGED_IN,
            route = Authentication.RouteTypes.GET,
            path = "/api/analysis/setHistory"
    )
    public void setHistory(Context ctx) {
        var model = new HashMap<String, Object>();
        try {
            List<String> historyList = history.getAllKeys();
            model.put("historyList", historyList);

            ctx.render("wiki/analysisHistoryFragment.ftl", model);

        } catch (Exception ex) {
            logger.error("Error running analysis pipeline with request body: " + ctx.body(), ex);
            ctx.status(500);
            ctx.render("defaultError.ftl");
        }
    }

    @Authentication(required = Authentication.Requirement.LOGGED_IN,
            route = Authentication.RouteTypes.POST,
            path = "/api/analysis/callHistory"
    )
    public void callHistory(Context ctx) {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        try {
            var requestDto = gson.fromJson(ctx.body(), HistoryRequestDto.class);

            var historyID = requestDto.getHistoryId();

            model.put("historyID", historyID);
            DUUIInformation duuiInformation = history.getDuuiInformation(historyID);
            String inputText = history.getInputText(historyID);
            List<String> selectedModels = history.getSelectedModels(historyID);
            ;
            String inputClaim = history.getInputClaim(historyID);
            String inputCoherence = history.getInputCoherence(historyID);
            String inputStance = history.getInputStance(historyID);
            String inputLLM = history.getInputLLM(historyID);

            model.put("DUUI", duuiInformation);
            model.put("SuccessRequest", true);
            model.put("modelGroups", duuiInformation.getModelGroups());
            model.put("inputText", inputText);
            model.put("selectedModels", selectedModels);
            model.put("historyID", historyID);
            model.put("inputClaim", inputClaim);
            model.put("inputCoherence", inputCoherence);
            model.put("inputStance", inputStance);
            model.put("inputLLM", inputLLM);
            // set history

            ctx.render("wiki/analysisResultFragment.ftl", model);

        } catch (Exception ex) {
            logger.error("Error running analysis pipeline with request body: " + ctx.body(), ex);
            ctx.status(500);
            ctx.render("defaultError.ftl");
        }
    }

    @Authentication(required = Authentication.Requirement.LOGGED_IN,
            route = Authentication.RouteTypes.POST,
            path = "/api/analysis/callHistoryText"
    )
    public void callHistoryText(Context ctx) {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        try {
            var requestDto = gson.fromJson(ctx.body(), HistoryRequestDto.class);
            var historyID = requestDto.getHistoryId();

            model.put("historyID", historyID);
            DUUIInformation duuiInformation = history.getDuuiInformation(historyID);
            String inputText = history.getInputText(historyID);
            List<String> selectedModels = history.getSelectedModels(historyID);
            ;

            model.put("DUUI", duuiInformation);
            model.put("SuccessRequest", true);
            model.put("modelGroups", duuiInformation.getModelGroups());
            model.put("inputText", inputText);
            model.put("selectedModels", selectedModels);
            model.put("historyID", historyID);
            model.put("inputClaim", history.getInputClaim(historyID));
            model.put("inputCoherence", history.getInputCoherence(historyID));
            model.put("inputStance", history.getInputStance(historyID));
            model.put("inputLLM", history.getInputLLM(historyID));
            // set history

            ctx.render("wiki/analysisHistoryTextFragment.ftl", model);

        } catch (Exception ex) {
            logger.error("Error running analysis pipeline with request body: " + ctx.body(), ex);
            ctx.status(500);
            ctx.render("defaultError.ftl");
        }
    }
    //  NEW IMPORT ROUTE (Javalin)
    @Authentication(required = Authentication.Requirement.LOGGED_IN,
            route = Authentication.RouteTypes.POST,
            path = "/api/analysis/importCas"
    )
    public Handler importCas = ctx -> {
        try {
            String analysisId = ctx.queryParam("analysisId");
            if (analysisId == null || analysisId.isBlank()) {
                ctx.status(400).result("Missing analysisId");
                return;
            }

            // Lookup cached session
            RunDUUIPipeline.AnalysisSession session = RunDUUIPipeline.getCachedSession(analysisId);
            if (session == null) {
                ctx.status(404).result("No cached CAS found for analysisId=" + analysisId);
                return;
            }

            // send to importer
            long corpusId = Long.parseLong(System.getenv().getOrDefault("UCE_IMPORT_CORPUS_ID", "1"));
            String documentId = null; // String documentId = "doc-" + analysisId;
            String casView = null;

            try {
                RunDUUIPipeline.sendToImporterViaHttp(
                        "http://localhost:4567/api/ie/upload/uima",
                        analysisId, corpusId, documentId, casView
                );
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Importer HTTP failed: " + e.getMessage());
                return;
            }

            ctx.status(200).result("CAS imported successfully for analysisId=" + analysisId);

        } catch (Exception ex) {
            logger.error("Error importing CAS", ex);
            ctx.status(500).result("Error importing CAS: " + ex.getMessage());
        }
    };
}
