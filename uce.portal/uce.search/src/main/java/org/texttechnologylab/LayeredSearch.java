package org.texttechnologylab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.dto.LayeredSearchLayerDto;
import org.texttechnologylab.models.dto.LayeredSearchSlotType;
import org.texttechnologylab.services.JenaSparqlService;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class LayeredSearch {

    private final String insertTemplateQuery = "INSERT INTO layered_search_temp.layered_search_{NAME} (id, document_id) \n" +
            "SELECT p.id, p.document_id\n" +
            "FROM page p\n" +
            "JOIN {TABLE} {ALIAS} ON {ALIAS}.page_id = p.id\n" +
            "WHERE {CONDITION} \n" +
            "ON CONFLICT (id) DO NOTHING;";
    private final String id;
    private List<LayeredSearchLayerDto> layers = new ArrayList<>();
    private final PostgresqlDataInterface_Impl db;
    private final JenaSparqlService jenaSparqlService;
    private static final Logger logger = LogManager.getLogger();

    public LayeredSearch(ApplicationContext serviceContext, String id) {
        this.id = id;
        this.jenaSparqlService = serviceContext.getBean(JenaSparqlService.class);
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
    }

    public String getId() {
        return this.id;
    }

    /**
     * Initalize this layered search. This setups the materialized view in the background and more.
     */
    public void init() throws DatabaseOperationException {
        createSearchTableIfNotExists(this.id + "_0");
    }

    /**
     * Takes in a layer of any depth and updates the corresponding layered search with the information.
     */
    public void updateLayers(ArrayList<LayeredSearchLayerDto> layerDtos) {
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
    public void executeLayersOnDb() {
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
        }
    }

    private boolean executeSingleLayerOnDb(LayeredSearchLayerDto layer) throws DatabaseOperationException {
        createSearchTableIfNotExists(this.id + "_" + layer.getDepth());
        var statements = new ArrayList<String>();

        for (var slot : layer.getSlots()) {

            if (slot.getType() == LayeredSearchSlotType.TAXON) {
                // Check if its a taxon command
                if (slot.getValue().length() > 2) {
                    var possibleCommand = slot.getValue().substring(0, 3);
                    if (Arrays.asList(StringUtils.TAX_RANKS).contains(possibleCommand)) {
                        // The full name of the taxonomic rank
                        var fullRankName = StringUtils.GetFullTaxonRankByCode(possibleCommand.replace("::", "")).toLowerCase();
                        var value = slot.getValue().substring(3);
                        //var ordinalValue = TaxonRank.valueOf(fullRankName).ordinal();
                        var idsOfRank = ExceptionUtils.tryCatchLog(
                                () -> jenaSparqlService.getIdsOfTaxonRank(fullRankName, value),
                                (ex) -> logger.error("Error fetching the biofid ids of a specific rank.", ex));
                        if (idsOfRank == null) continue;

                        var condition = "b.{RANK_NAME} IN ({ID_LIST}) GROUP BY p.id, p.document_id HAVING COUNT(b.page_id) > 0";
                        condition = condition.replace("{RANK_NAME}", fullRankName);
                        condition = condition.replace("{ID_LIST}", String.join(",", idsOfRank.stream().map(i -> "'" + i + "'").toList()));

                        var sql = insertTemplateQuery;
                        sql = sql.replace("{NAME}", this.id + "_" + layer.getDepth());
                        sql = sql.replace("{TABLE}", "biofidtaxon");
                        sql = sql.replace("{ALIAS}", "b");
                        sql = sql.replace("{CONDITION}", condition);
                        statements.add(sql);
                    }
                }
            }
        }

        // Now execute the statements on our view
        for(var statement:statements) db.executeSqlWithoutReturn(statement);

        return true;
    }

    private void createSearchTableIfNotExists(String name) throws DatabaseOperationException {
        var query = "CREATE SCHEMA IF NOT EXISTS layered_search_temp;\n" +
                "DO $$ \n" +
                "BEGIN\n" +
                "    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'layered_search_temp' AND table_name = 'layered_search_{NAME}') THEN\n" +
                "        CREATE TABLE layered_search_temp.layered_search_{NAME} (\n" +
                "            id BIGINT PRIMARY KEY, \n" +
                "            document_id BIGINT\n" +
                "        );\n" +
                "    END IF;\n" +
                "END $$;\n";
        query = query.replace("{NAME}", name);
        db.executeSqlWithoutReturn(query);
    }

}
