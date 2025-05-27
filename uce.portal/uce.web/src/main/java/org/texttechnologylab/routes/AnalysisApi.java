package org.texttechnologylab.routes;
import freemarker.template.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.models.dto.AnalysisRequestDto;
import org.texttechnologylab.models.dto.HistoryRequestDto;
import spark.Route;
import com.google.gson.Gson;
import org.texttechnologylab.*;
import java.util.HashMap;
import java.util.List;

import org.texttechnologylab.modules.DUUIInformation;


import spark.ModelAndView;


public class AnalysisApi {
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


    public Route runPipeline = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        try {
            var requestDto = gson.fromJson(request.body(), AnalysisRequestDto.class);

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
            DUUIInformation DataRequest = pipeline.getModelResources(selectedModels, inputText, inputClaim, inputCoherence, inputStance, inputLLM);
            model.put("DUUI", DataRequest);
            model.put("SuccessRequest", true);
            model.put("modelGroups", DataRequest.getModelGroups());
            // set history

            history.addDuuiInformation(String.valueOf(counter), DataRequest);
            history.setModelGroupHashMap(String.valueOf(counter), DataRequest.getModelGroups());
            history.addInputText(String.valueOf(counter), inputText);
            history.addSelectedModels(String.valueOf(counter), selectedModels);
            history.addInputClaim(String.valueOf(counter),inputClaim);
            history.addInputCoherence(String.valueOf(counter), inputCoherence);
            history.addInputStance(String.valueOf(counter), inputStance);
            history.addInputLLM(String.valueOf(counter), inputLLM);
            counter++;

            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(model, "wiki/analysisResultFragment.ftl"));

        } catch (Exception ex) {
            logger.error("Error running analysis pipeline with request body: " + request.body(), ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

    public Route setHistory = ((request, response) -> {
        var model = new HashMap<String, Object>();
        try {
            List<String> historyList = history.getAllKeys();
            model.put("historyList", historyList);

            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(model, "wiki/analysisHistoryFragment.ftl"));

        } catch (Exception ex) {
            logger.error("Error running analysis pipeline with request body: " + request.body(), ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

    public Route callHistory = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        try {
            var requestDto = gson.fromJson(request.body(), HistoryRequestDto.class);

            var historyID = requestDto.getHistoryId();

            model.put("historyID", historyID);
            DUUIInformation duuiInformation = history.getDuuiInformation(historyID);
            String inputText = history.getInputText(historyID);
            List<String> selectedModels = history.getSelectedModels(historyID);;
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

            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(model, "wiki/analysisResultFragment.ftl"));

        } catch (Exception ex) {
            logger.error("Error running analysis pipeline with request body: " + request.body(), ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

    public Route callHistoryText = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        try {
            var requestDto = gson.fromJson(request.body(), HistoryRequestDto.class);
            var historyID = requestDto.getHistoryId();

            model.put("historyID", historyID);
            DUUIInformation duuiInformation = history.getDuuiInformation(historyID);
            String inputText = history.getInputText(historyID);
            List<String> selectedModels = history.getSelectedModels(historyID);;

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

            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(model, "wiki/analysisHistoryTextFragment.ftl"));

        } catch (Exception ex) {
            logger.error("Error running analysis pipeline with request body: " + request.body(), ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

}
