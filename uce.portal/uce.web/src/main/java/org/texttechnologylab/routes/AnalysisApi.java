package org.texttechnologylab.routes;
import freemarker.template.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.models.AnalysisRequestDto;
import org.texttechnologylab.models.HistoryRequestDto;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import spark.Route;
import com.google.gson.Gson;
import org.texttechnologylab.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

            var selectedModels = requestDto.getSelectedModels(); // Liste der IDs
            var inputText = requestDto.getInputText();           // Eingabetext
            var inputClaim = requestDto.getInputClaim();                 // Eingabewort
            var inputCoherence = requestDto.getInputCoherence(); // Eingabewort

            model.put("inputText", inputText);
            model.put("selectedModels", selectedModels);
            model.put("inputClaim", inputClaim);
            model.put("inputCoherence", inputCoherence);

            RunDUUIPipeline pipeline = new RunDUUIPipeline();
            DUUIInformation  DataRequest = pipeline.getModelResources(selectedModels, inputText, inputClaim, inputCoherence);
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
            counter++;

            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(model, "wiki/analysis-result-fragment.ftl"));

        } catch (Exception ex) {
            logger.error("Error running analysis pipeline with request body: " + request.body(), ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

    public Route setHistory = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        try {
            List<String> historyList = history.getAllKeys();
            model.put("historyList", historyList);

            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(model, "wiki/analysis-history-fragment.ftl"));

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

            var historyID = requestDto.getHistoryId();          // Eingabetext

            model.put("historyID", historyID);
            DUUIInformation duuiInformation = history.getDuuiInformation(historyID);
            String inputText = history.getInputText(historyID);
            List<String> selectedModels = history.getSelectedModels(historyID);;
            String inputClaim = history.getInputClaim(historyID);
            String inputCoherence = history.getInputCoherence(historyID);

            model.put("DUUI", duuiInformation);
            model.put("SuccessRequest", true);
            model.put("modelGroups", duuiInformation.getModelGroups());
            model.put("inputText", inputText);
            model.put("selectedModels", selectedModels);
            model.put("historyID", historyID);
            model.put("inputClaim", inputClaim);
            model.put("inputCoherence", inputCoherence);
            // set history

            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(model, "wiki/analysis-result-fragment.ftl"));

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
            var historyID = requestDto.getHistoryId();          // Eingabetext

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
            // set history

            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(model, "wiki/analysis-history-text-fragment.ftl"));

        } catch (Exception ex) {
            logger.error("Error running analysis pipeline with request body: " + request.body(), ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

}
