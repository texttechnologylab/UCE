package org.texttechnologylab.routes;
import freemarker.template.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.models.AnalysisRequestDto;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import spark.Route;
import com.google.gson.Gson;
import org.texttechnologylab.*;
import java.util.HashMap;
import java.util.Map;

import spark.ModelAndView;


public class AnalysisApi {
    private static final Logger logger = LogManager.getLogger(AnalysisApi.class);
    private ApplicationContext context = null;
    private Configuration freemarkerConfig;

    public AnalysisApi(ApplicationContext context, Configuration freemarkerConfig) {
        this.context = context;
        this.freemarkerConfig = freemarkerConfig;
    }


    public Route runPipeline = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var gson = new Gson();
        try {
            var requestDto = gson.fromJson(request.body(), AnalysisRequestDto.class);

            var selectedModels = requestDto.getSelectedModels(); // Liste der IDs
            var inputText = requestDto.getInputText();           // Eingabetext

            model.put("inputText", inputText);
            model.put("selectedModels", selectedModels);

            RunDUUIPipeline pipeline = new RunDUUIPipeline();
            DUUIInformation  DataRequest = pipeline.getModelResources(selectedModels, inputText);
            model.put("DUUI", DataRequest);
            model.put("SuccessRequest", true);
            model.put("modelGroups", DataRequest.getModelGroups());

            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(model, "wiki/analysis-result-fragment.ftl"));

        } catch (Exception ex) {
            logger.error("Error running analysis pipeline with request body: " + request.body(), ex);
            response.status(500);
            return new CustomFreeMarkerEngine(this.freemarkerConfig)
                    .render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

}
