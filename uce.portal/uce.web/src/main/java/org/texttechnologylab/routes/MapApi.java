package org.texttechnologylab.routes;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.CustomFreeMarkerEngine;
import org.texttechnologylab.LanguageResources;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.services.*;
import spark.ModelAndView;
import spark.Route;

import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

public class MapApi {

    private static final Logger logger = LogManager.getLogger(WikiApi.class);
    private Configuration freemarkerConfig;
    private final PostgresqlDataInterface_Impl db;
    private MapService mapService;
    private final Gson gson = new Gson();

    public MapApi(ApplicationContext serviceContext, Configuration freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
        this.mapService = serviceContext.getBean(MapService.class);
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
    }

    public Route getLinkedOccurrences = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var languageResources = LanguageResources.fromRequest(request);
        var requestBody = gson.fromJson(request.body(), Map.class);

        var minLng = ExceptionUtils.tryCatchLog(() -> Double.parseDouble(requestBody.get("minLng").toString()),
                (ex) -> logger.error("Couldn't fetch occurrences of map - minLng missing.", ex));
        var minLat = ExceptionUtils.tryCatchLog(() -> Double.parseDouble(requestBody.get("minLat").toString()),
                (ex) -> logger.error("Couldn't fetch occurrences of map - minLat missing.", ex));
        var maxLng = ExceptionUtils.tryCatchLog(() -> Double.parseDouble(requestBody.get("maxLng").toString()),
                (ex) -> logger.error("Couldn't fetch occurrences of map - maxLng missing.", ex));
        var maxLat = ExceptionUtils.tryCatchLog(() -> Double.parseDouble(requestBody.get("maxLat").toString()),
                (ex) -> logger.error("Couldn't fetch occurrences of map - maxLat missing.", ex));
        var skip = ExceptionUtils.tryCatchLog(() -> (int)Double.parseDouble(requestBody.get("skip").toString()), (ex) -> {});
        var take = ExceptionUtils.tryCatchLog(() -> (int)Double.parseDouble(requestBody.get("take").toString()), (ex) -> {});
        var fromDate = ExceptionUtils.tryCatchLog(() -> Date.valueOf(requestBody.get("fromDate").toString()), (ex) -> {});
        var toDate = ExceptionUtils.tryCatchLog(() -> Date.valueOf(requestBody.get("toDate").toString()), (ex) -> {});
        var corpusId = ExceptionUtils.tryCatchLog(() -> (long)Double.parseDouble(requestBody.get("corpusId").toString()),
                (ex) -> logger.error("Couldn't fetch occurrences of map - corpusId missing.", ex));

        if(minLng == null || minLat == null || maxLng == null || maxLat == null || corpusId == null){
            model.put("information", languageResources.get("missingParameterError"));
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "defaultError.ftl"));
        }

        if(skip == null) skip = 0;
        if(take == null) take = 25;

        try {
            return gson.toJson(mapService.getGeoNameTimelineLinks(minLng, minLat, maxLng, maxLat, fromDate, toDate, corpusId, skip, take));
        } catch (Exception ex) {
            logger.error("Error getting linked occurrences from map - best refer to the last logged API call " +
                         "with id=" + request.attribute("id") + " to this endpoint for URI parameters.", ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

    public Route getLinkedOccurrenceClusters = ((request, response) -> {
        var model = new HashMap<String, Object>();
        var languageResources = LanguageResources.fromRequest(request);
        var requestBody = gson.fromJson(request.body(), Map.class);

        var minLng = ExceptionUtils.tryCatchLog(() -> Double.parseDouble(requestBody.get("minLng").toString()),
                (ex) -> logger.error("Couldn't fetch occurrences of map - minLng missing.", ex));
        var minLat = ExceptionUtils.tryCatchLog(() -> Double.parseDouble(requestBody.get("minLat").toString()),
                (ex) -> logger.error("Couldn't fetch occurrences of map - minLat missing.", ex));
        var maxLng = ExceptionUtils.tryCatchLog(() -> Double.parseDouble(requestBody.get("maxLng").toString()),
                (ex) -> logger.error("Couldn't fetch occurrences of map - maxLng missing.", ex));
        var maxLat = ExceptionUtils.tryCatchLog(() -> Double.parseDouble(requestBody.get("maxLat").toString()),
                (ex) -> logger.error("Couldn't fetch occurrences of map - maxLat missing.", ex));
        var zoom = ExceptionUtils.tryCatchLog(() -> Double.parseDouble(requestBody.get("zoom").toString()),
                (ex) -> logger.error("Couldn't fetch occurrences of map - gridSize missing.", ex));
        var fromDate = ExceptionUtils.tryCatchLog(() -> Date.valueOf(requestBody.get("fromDate").toString()), (ex) -> {});
        var toDate = ExceptionUtils.tryCatchLog(() -> Date.valueOf(requestBody.get("toDate").toString()), (ex) -> {});
        var corpusId = ExceptionUtils.tryCatchLog(() -> (long)Double.parseDouble(requestBody.get("corpusId").toString()),
                (ex) -> logger.error("Couldn't fetch occurrences of map - corpusId missing.", ex));

        if(minLng == null || minLat == null || maxLng == null || maxLat == null || zoom == null || corpusId == null){
            model.put("information", languageResources.get("missingParameterError"));
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(model, "defaultError.ftl"));
        }

        try {
            return gson.toJson(mapService.getTimelineMapClusters(minLng, minLat, maxLng, maxLat, 0.0001, fromDate, toDate, corpusId));
        } catch (Exception ex) {
            logger.error("Error getting linked occurrence clusters from map - best refer to the last logged API call " +
                         "with id=" + request.attribute("id") + " to this endpoint for URI parameters.", ex);
            return new CustomFreeMarkerEngine(this.freemarkerConfig).render(new ModelAndView(null, "defaultError.ftl"));
        }
    });

}
