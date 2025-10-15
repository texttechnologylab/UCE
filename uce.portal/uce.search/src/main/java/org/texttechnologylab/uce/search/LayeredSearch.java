package org.texttechnologylab.uce.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.uce.common.exceptions.DatabaseOperationException;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.models.corpus.GeoNameFeatureClass;
import org.texttechnologylab.uce.common.models.dto.LayeredSearchLayerDto;
import org.texttechnologylab.uce.common.models.dto.LayeredSearchSlotType;
import org.texttechnologylab.uce.common.models.search.CacheItem;
import org.texttechnologylab.uce.common.models.search.EnrichedSearchQuery;
import org.texttechnologylab.uce.common.services.JenaSparqlService;
import org.texttechnologylab.uce.common.services.PostgresqlDataInterface_Impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

// TODO: This class needs cleanup. I did this in such a haste and under time pressure.
public class LayeredSearch extends CacheItem {

    private final String id;
    private List<LayeredSearchLayerDto> layers = new ArrayList<>();
    private final PostgresqlDataInterface_Impl db;
    private final JenaSparqlService jenaSparqlService;
    private static final Logger logger = LogManager.getLogger(LayeredSearch.class);

    /**
     * Cleans up and deletes all existing tables within the 'search' schema of the database.
     */
    public static void CleanupScheme(PostgresqlDataInterface_Impl db) throws DatabaseOperationException {
        var query = "DO $$ DECLARE\n" +
                    "    r RECORD;\n" +
                    "BEGIN\n" +
                    "    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'search') LOOP\n" +
                    "        EXECUTE 'DROP TABLE IF EXISTS search.' || quote_ident(r.tablename) || ' CASCADE';\n" +
                    "    END LOOP;\n" +
                    "END $$;";
        db.executeSqlWithoutReturn(query);
    }

    public LayeredSearch(ApplicationContext serviceContext, String id) {
        this.id = id;
        this.jenaSparqlService = serviceContext.getBean(JenaSparqlService.class);
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
    }

    public String getId() {
        return this.id;
    }

    public List<LayeredSearchLayerDto> getLayers() {
        return this.layers;
    }

    public String getFinalLayerTableName() {
        var maxDepth = this.layers.stream().max(Comparator.comparing(LayeredSearchLayerDto::getDepth)).get().getDepth();
        return buildLayerTableName(maxDepth);
    }

    /**
     * Initalize this layered search. This setups the materialized view in the background and more.
     */
    public void init() {
    }

    /**
     * Disposes this @LayeredSearch by also cleaning up temporary tables in the database.
     */
    public void dispose() throws DatabaseOperationException {
        dropAllTables();
    }

    /**
     * Takes in a layer of any depth and updates the corresponding layered search with the information.
     */
    public void updateLayers(ArrayList<LayeredSearchLayerDto> layerDtos) throws DatabaseOperationException {
        for (var layer : layerDtos) {
            var existingLayer = this.layers.stream().filter(l -> l.getDepth() == layer.getDepth()).findFirst();
            // If we have a layer of that depth, check if its dirty (the slots changed its value)
            if (existingLayer.isPresent()) {
                // If the hashes of the slots changed, this layer needs to be applied again as the sql will change.
                layer.calculateSlotsHash();
                existingLayer.get().calculateSlotsHash();
                if (!existingLayer.get().getSlotsHash().equals(layer.getSlotsHash())) {
                    existingLayer.get().setSlots(layer.getSlots());
                    existingLayer.get().setDirty(true);
                }
            } else {
                layer.setDirty(true);
                this.layers.add(layer);
            }
        }

        this.executeLayersOnDb();
    }

    /**
     * This looks at the existing layers and, if necessary, applies and updates new and existing sql queries of the layers
     */
    public void executeLayersOnDb() throws DatabaseOperationException {
        // If no layers are dirty, we don't have to do anything
        if (this.layers.stream().noneMatch(LayeredSearchLayerDto::isDirty)) return;

        // Else, we need to know the layer with the smallest depth that is dirty, since all the following
        // layers need to be applied again.
        var smallestDirtyLayer = this.layers.stream()
                .filter(LayeredSearchLayerDto::isDirty)
                .min(Comparator.comparingInt(LayeredSearchLayerDto::getDepth))
                .orElse(null);
        // This shouldn't be possible.
        if (smallestDirtyLayer == null) return;

        var layersToUpdate = this.layers.stream().filter(l -> l.getDepth() >= smallestDirtyLayer.getDepth()).toList();
        for (var layer : layersToUpdate) {
            var result = ExceptionUtils.tryCatchLog(
                    () -> this.executeSingleLayerOnDb(layer),
                    (ex) -> logger.error("Error executing a layer update on the db.", ex));
            if (result != null && result) {
                layer.setDirty(false);
                calculateLayerCount(layer);
            }
        }
    }

    private boolean executeSingleLayerOnDb(LayeredSearchLayerDto layer) throws DatabaseOperationException {
        dropTable(buildLayerTableName(layer.getDepth()));
        createSearchTableIfNotExists(buildLayerTableName(layer.getDepth()));

        var insertTemplateQuery = "INSERT INTO search.{NAME} (id, document_id, begin_end) \n" +
                                  "SELECT p.id, p.document_id, jsonb_agg(jsonb_build_array({ALIAS}.beginn, {ALIAS}.endd)) AS begin_end \n" +
                                  "FROM {SOURCE} p\n" +
                                  "JOIN {TABLE} {ALIAS} ON {ALIAS}.page_id = p.id\n" +
                                  "WHERE {CONDITION} \n" +
                                  "ON CONFLICT (id) DO NOTHING;";
        var conditionEnding = "GROUP BY p.id, p.document_id HAVING COUNT(a.page_id) > 0";
        var statements = new ArrayList<String>();

        for (var slot : layer.getSlots()) {
            var sql = insertTemplateQuery;
            sql = sql.replace("{NAME}", buildLayerTableName(layer.getDepth()));
            sql = sql.replace("{ALIAS}", "a");
            sql = sql.replace("{SOURCE}", layer.getDepth() == 1 ? "page" : "search." + buildLayerTableName(layer.getDepth() - 1));

            if (slot.getType() == LayeredSearchSlotType.TAXON) {
                sql = sql.replace("{TABLE}", "biofidtaxon");

                // Check if its a taxon command
                if (slot.getValue().length() > 2) {
                    var possibleCommand = slot.getValue().substring(0, 3);
                    if (Arrays.asList(EnrichedSearchQuery.TAX_RANKS).contains(possibleCommand)) {
                        // The full name of the taxonomic rank
                        var fullRankName = EnrichedSearchQuery.getFullTaxonRankByCode(possibleCommand.replace("::", "")).toLowerCase();
                        var value = slot.getValue().substring(3);
                        //var ordinalValue = TaxonRank.valueOf(fullRankName).ordinal();
                        var idsOfRank = ExceptionUtils.tryCatchLog(
                                () -> jenaSparqlService.getIdsOfTaxonRank(fullRankName, value),
                                (ex) -> logger.error("Error fetching the biofid ids of a specific rank.", ex));
                        if (idsOfRank == null || idsOfRank.isEmpty()) continue;

                        var condition = "a.{RANK_NAME} IN ({ID_LIST}) AND a.page_id is not null " + conditionEnding;
                        condition = condition.replace("{RANK_NAME}", fullRankName);
                        condition = condition.replace("{ID_LIST}", String.join(",", idsOfRank.stream().map(i -> "'" + i + "'").toList()));

                        var statement = sql.replace("{CONDITION}", condition);
                        statements.add(statement);
                        continue;
                    }
                }

                // If it's not a taxon command, then we are just looking for taxons by their primary name
                var condition = "a.primaryname = E'{VALUE}' AND a.page_id is not null " + conditionEnding;
                condition = condition.replace("{VALUE}", slot.getCleanedValue());
                var statement = sql.replace("{CONDITION}", condition);
                statements.add(statement);
            } else if (slot.getType() == LayeredSearchSlotType.TIME) {
                // Handle the time slots.
                sql = sql.replace("{TABLE}", "time");

                // Let's see if we got a range in here!
                if (slot.getValue().contains("-")) {
                    var split = slot.getValue().split("-");
                    var from = split[0].trim();
                    var to = split[1].trim();

                    var condition = "a.year >= {FROM} and a.year <= {TO} AND a.page_id is not null " + conditionEnding;
                    condition = condition.replace("{FROM}", from);
                    condition = condition.replace("{TO}", to);
                    var statement = sql.replace("{CONDITION}", condition);
                    statements.add(statement);
                    continue;
                }

                // Let's see if we too have some commands in here.
                if (slot.getValue().length() > 2) {
                    var possibleCommand = slot.getValue().substring(0, 3);
                    if (Arrays.asList(EnrichedSearchQuery.TIME_COMMANDS).contains(possibleCommand)) {
                        // The full name of the taxonomic rank
                        var unitName = EnrichedSearchQuery.getFullTimeUnitByCode(possibleCommand.replace("::", "")).toLowerCase();
                        var value = slot.getValue().substring(3);

                        var condition = "a.{UNIT_NAME} = {VALUE} AND a.page_id is not null " + conditionEnding;
                        condition = condition.replace("{UNIT_NAME}", unitName);
                        if (unitName.equals("year")) condition = condition.replace("{VALUE}", value);
                        else condition = condition.replace("{VALUE}", "'" + value + "'");

                        var statement = sql.replace("{CONDITION}", condition);
                        statements.add(statement);
                        continue;
                    }
                }

                // If it's not a command, then we are just looking by string
                var condition = "a.coveredtext = E'{VALUE}' " + conditionEnding;
                condition = condition.replace("{VALUE}", slot.getCleanedValue());
                var statement = sql.replace("{CONDITION}", condition);
                statements.add(statement);
            } else if (slot.getType() == LayeredSearchSlotType.LOCATION) {
                sql = sql.replace("{TABLE}", "geoname");

                // Let's see if we got a specific long/lat with a range
                if (slot.getValue().startsWith("R::")) {
                    var locationDto = EnrichedSearchQuery.parseLocationRadiusCommand(slot.getValue());
                    if (locationDto == null) continue;
                    // We use the postgis extension for fast radius-based geographic queries.
                    var condition = "ST_DWithin(location_geog, ST_MakePoint({LNG},{LAT}), {RADIUS}) " + conditionEnding;
                    condition = condition.replace("{LNG}", Double.toString(locationDto.getLongitude()));
                    condition = condition.replace("{LAT}", Double.toString(locationDto.getLatitude()));
                    condition = condition.replace("{RADIUS}", Double.toString(locationDto.getRadius()));
                    var statement = sql.replace("{CONDITION}", condition);
                    statements.add(statement);
                    continue;
                } else if (slot.getValue().startsWith("LOC::")) {
                    // Then we have a location command with geonames feature classes and codes. Have a look at EnrichedSearchQuery for better description
                    var split = slot.getValue().replace("LOC::", "").split("\\.");
                    var featureClass = split[0];
                    var featureCode = "";
                    if (split.length > 1) featureCode = split[1];

                    var condition = "a.featureclass = {FEATURE_CLASS} ".replace("{FEATURE_CLASS}", Integer.toString(GeoNameFeatureClass.valueOf(featureClass).ordinal()));
                    if (!featureCode.isEmpty())
                        condition += "AND a.featurecode = '{FEATURE_CODE}' ".replace("{FEATURE_CODE}", featureCode);
                    condition += "AND a.page_id is not null " + conditionEnding;
                    var statement = sql.replace("{CONDITION}", condition);
                    statements.add(statement);
                }
            }
        }

        // Now execute the statements on our view
        for (var statement : statements) db.executeSqlWithoutReturn(statement);

        return true;
    }

    private void createSearchTableIfNotExists(String name) throws DatabaseOperationException {
        var query = "CREATE SCHEMA IF NOT EXISTS search;\n" +
                    "DO $$ \n" +
                    "BEGIN\n" +
                    "    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'search' AND table_name = '{NAME}') THEN\n" +
                    "        CREATE TABLE search.{NAME} (\n" +
                    "            id BIGINT PRIMARY KEY, \n" +
                    "            document_id BIGINT,\n" +
                    "            begin_end jsonb\n" +
                    "        );\n" +
                    "    END IF;\n" +
                    "END $$;\n";
        query = query.replace("{NAME}", name);
        db.executeSqlWithoutReturn(query);
    }

    private void calculateLayerCount(LayeredSearchLayerDto layer) throws DatabaseOperationException {
        var query = "SELECT COUNT(DISTINCT id) AS p_count, COUNT(DISTINCT document_id) as d_count FROM search." + buildLayerTableName(layer.getDepth());
        var resultList = db.executeSqlWithReturn(query);
        if (resultList.isEmpty()) return;
        else {
            var counts = (Object[]) resultList.getFirst();
            layer.setPageHits(((Number) counts[0]).intValue());
            layer.setDocumentHits(((Number) counts[1]).intValue());
        }
    }

    /**
     * Drops all tables that start with the given searchId, so all depths of it.
     */
    private void dropAllTables() throws DatabaseOperationException {
        var query = "DO $$ \n" +
                    "DECLARE \n" +
                    "    table_name TEXT;\n" +
                    "BEGIN\n" +
                    "    FOR table_name IN \n" +
                    "        SELECT tablename FROM pg_tables WHERE schemaname = 'search' AND tablename LIKE '{PREFIX}%'\n" +
                    "    LOOP\n" +
                    "        EXECUTE 'DROP TABLE IF EXISTS search.' || table_name || ' CASCADE';\n" +
                    "    END LOOP;\n" +
                    "END $$;";
        query = query.replace("{PREFIX}", "layered_search_" + this.id);
        db.executeSqlWithoutReturn(query);
    }

    private void dropTable(String name) throws DatabaseOperationException {
        var query = "DROP TABLE IF EXISTS search.{NAME}";
        query = query.replace("{NAME}", name);
        db.executeSqlWithoutReturn(query);
    }

    private String buildLayerTableName(int depth) {
        return "layered_search_" + this.id + "_" + depth;
    }

}
