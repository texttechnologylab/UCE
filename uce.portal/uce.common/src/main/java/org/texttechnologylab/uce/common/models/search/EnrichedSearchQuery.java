package org.texttechnologylab.uce.common.models.search;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.texttechnologylab.uce.common.config.CommonConfig;
import org.texttechnologylab.uce.common.exceptions.DatabaseOperationException;
import org.texttechnologylab.uce.common.models.corpus.GeoNameFeatureClass;
import org.texttechnologylab.uce.common.models.dto.map.LocationDto;
import org.texttechnologylab.uce.common.models.viewModels.CorpusViewModel;
import org.texttechnologylab.uce.common.services.JenaSparqlService;
import org.texttechnologylab.uce.common.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.uce.common.utils.StringUtils;
import org.texttechnologylab.uce.common.utils.SystemStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Object class that, given a user input, returns an Enriched version of that input parsing
 * potential commands and references.
 */
public class EnrichedSearchQuery {
    public static final String[] QUERY_OPERATORS = {"&", "|", "!", "<->", "(", ")"};
    // https://en.wikipedia.org/wiki/Taxonomic_rank#:~:text=Main%20ranks,-In%20his%20landmark&text=Today%2C%20the%20nomenclature%20is%20regulated,family%2C%20genus%2C%20and%20species.
    public static final String[] TAX_RANKS = {"G::", "F::", "O::", "C::", "P::", "K::", "S::"};
    public static final String[] LOCATION_COMMANDS = {"LOC::", "R::"};
    public static final String[] TIME_COMMANDS = {"Y::", "M::", "D::", "E::", "T::"};
    private static final CommonConfig config = new CommonConfig();

    public static String getFullTaxonRankByCode(String code) {
        return switch (code) {
            case "C" -> "class";
            case "F" -> "family";
            case "K" -> "kingdom";
            case "P" -> "phylum";
            case "O" -> "order";
            case "G" -> "genus";
            case "S" -> "species";
            default -> null;
        };
    }

    public static String getFullTimeUnitByCode(String code) {
        return switch (code) {
            case "Y" -> "year";
            case "M" -> "month";
            case "D" -> "day";
            case "E" -> "season";
            case "T" -> "range";
            default -> null;
        };
    }

    private final PostgresqlDataInterface_Impl db;
    private final JenaSparqlService jenaSparqlService;

    @Getter
    private final String originalQuery;
    @Getter
    private String enrichedQuery;
    @Getter
    private boolean enrichedQueryIsCutOff;
    @Getter
    private List<EnrichedSearchToken> enrichedSearchTokens;
    private boolean parseTaxonomy;
    private boolean parseGeonames;
    private boolean parseTimes;

    public EnrichedSearchQuery(String query,
                               PostgresqlDataInterface_Impl db,
                               JenaSparqlService jenaSparqlService) {
        this.originalQuery = query;
        this.jenaSparqlService = jenaSparqlService;
        this.db = db;
    }

    public EnrichedSearchQuery parse(boolean proModeEnabled, long corpusId) throws DatabaseOperationException, IOException {
        var corpusVm = db.getCorpusById(corpusId).getViewModel();
        var searchQuery = StringUtils.replaceSpacesInQuotes(this.originalQuery);
        var tokens = searchQuery.split(" ");
        var delimiter = proModeEnabled ? "'" : "\"";
        var or = proModeEnabled ? " | " : " or ";

        this.enrichedSearchTokens = new ArrayList<>();
        var enrichedSearchQuery = new StringBuilder();

        for (var token : tokens) {
            if (isOperator(token)) {
                appendOperator(token, enrichedSearchQuery);
                continue;
            }

            var cleanedToken = cleanToken(token);
            var enrichedToken = new EnrichedSearchToken();
            enrichedToken.setValue(cleanedToken);

            boolean isEnriched = false;

            // First we try to parse the command for potential enrichment
            if (shouldHandleLocationCommand(cleanedToken, corpusVm)) {
                isEnriched = handleLocationCommand(cleanedToken, enrichedToken, enrichedSearchQuery, delimiter, or, corpusVm.getCorpus().getId());
            } else if (shouldHandleTimeCommand(cleanedToken, corpusVm)) {
                isEnriched = handleTimesCommand(cleanedToken, enrichedToken, enrichedSearchQuery, delimiter, or, corpusVm.getCorpus().getId());
            } else if (shouldHandleTaxonomicCommand(cleanedToken)) {
                isEnriched = handleTaxonomicCommand(cleanedToken, enrichedToken, enrichedSearchQuery, delimiter, or);
            }

            // If none of the commands fit, we look for alternative names and synonyms "manually"
            if (!isEnriched && this.parseTaxonomy && SystemStatus.JenaSparqlStatus.isAlive()) {
                isEnriched = handleBasicTaxon(cleanedToken, token, enrichedToken, enrichedSearchQuery, delimiter, or);
            }

            enrichedSearchTokens.add(enrichedToken);
            if (!isEnriched) enrichedSearchQuery.append(delimiter).append(cleanedToken).append(delimiter);
        }

        this.enrichedQuery = enrichedSearchQuery.toString().trim();
        this.enrichedSearchTokens = enrichedSearchTokens.stream()
                .filter(t -> !t.getValue().trim().isBlank())
                .toList();

        return this;
    }

    private boolean isOperator(String token) {
        return Arrays.asList(QUERY_OPERATORS).contains(token);
    }

    private void appendOperator(String token, @NotNull StringBuilder query) {
        query.append(token).append(" ");
        enrichedSearchTokens.add(new EnrichedSearchToken(token, EnrichedSearchTokenType.OPERATOR));
    }

    private String cleanToken(String token) {
        var cleaned = StringUtils.removeSpecialCharactersAtEdges(token);
        return cleaned.contains("__") ? cleaned.replaceAll("__", " ") : cleaned;
    }

    private boolean shouldHandleTimeCommand(String token, @NotNull CorpusViewModel corpusVm) {
        return corpusVm.getCorpusConfig().getAnnotations().isTime()
               && this.parseTimes
               && Stream.of(TIME_COMMANDS).anyMatch(token::startsWith);
    }

    private boolean shouldHandleLocationCommand(String token, @NotNull CorpusViewModel corpusVm) {
        return corpusVm.getCorpusConfig().getAnnotations().isGeoNames()
               && this.parseGeonames
               && Stream.of(LOCATION_COMMANDS).anyMatch(token::startsWith);
    }

    private boolean shouldHandleTaxonomicCommand(String token) {
        return SystemStatus.JenaSparqlStatus.isAlive()
               && this.parseTaxonomy
               && Stream.of(TAX_RANKS).anyMatch(token::startsWith);
    }

    private boolean handleLocationCommand(@NotNull String token,
                                          @NotNull EnrichedSearchToken enrichedToken,
                                          StringBuilder query,
                                          String delimiter,
                                          String or,
                                          long corpusId) throws DatabaseOperationException {
        enrichedToken.setType(EnrichedSearchTokenType.LOCATION_COMMAND);
        var command = "";
        for (var c : LOCATION_COMMANDS) {
            if (token.startsWith(c)) command = c;
        }
        var value = StringUtils.removeSpecialCharactersAtEdges(token.substring(command.length()));
        enrichedToken.setValue(value);

        // Handle the different commands
        List<String> geoNames = new ArrayList<>();

        if (command.equals("R::")) {
            // Syntax should be R::lng=5;lat=70;r=1000
            var locationDto = parseLocationRadiusCommand(value);
            geoNames = db.getDistinctGeonamesNamesByRadius(locationDto.getLongitude(), locationDto.getLatitude(), locationDto.getRadius(), corpusId, config.getLocationEnrichmentLimit());
        } else if (command.equals("LOC::")) {
            // The syntax should be  LOC::<FEATURE_CLASS>.<FEATURE_CODE>, so e.g.: LOC::A.ADMS
            var split = value.split("\\.");
            var featureClass = split[0];
            var featureCode = "";
            if (split.length > 1) featureCode = split[1];
            geoNames = db.getDistinctGeonamesNamesByFeatureCode(GeoNameFeatureClass.valueOf(featureClass), featureCode, corpusId, config.getLocationEnrichmentLimit());
        }

        if(geoNames.size() >= config.getLocationEnrichmentLimit()) this.enrichedQueryIsCutOff = true;

        if (geoNames.isEmpty()) {
            query.append(delimiter).append(value).append(delimiter).append(" ");
        } else {
            appendEnrichedNames(query, geoNames, "", delimiter, or);
            enrichedToken.setChildren(geoNames.stream()
                    .map(n -> new EnrichedSearchToken(n, EnrichedSearchTokenType.LOCATION)).toList());
        }

        return true;
    }

    public static LocationDto parseLocationRadiusCommand(@NotNull String input) {
        if (input.startsWith("R::")) input = input.replace("R::", "");
        return LocationDto.fromCommandString(input);
    }

    private boolean handleTimesCommand(
            @NotNull String token,
            @NotNull EnrichedSearchToken enrichedToken,
            StringBuilder query,
            String delimiter,
            String or,
            long corpusId
    ) throws DatabaseOperationException {
        enrichedToken.setType(EnrichedSearchTokenType.TIME_COMMAND);

        var command = token.substring(0, 3);
        var value = StringUtils.removeSpecialCharactersAtEdges(token.substring(3));
        enrichedToken.setValue(value);

        var unitCode = command.replace("::", "");
        var unitName = getFullTimeUnitByCode(unitCode).toLowerCase();

        String condition;

        if (!unitName.equals("range")) {
            // For units like year, month, day, season
            var formattedValue = unitName.equals("year") ? value : "'" + value + "'";
            condition = String.format("t.%s = %s AND t.pageId IS NOT NULL", unitName, formattedValue);
        } else if (value.contains("-")) {
            // Handle range like 2010-2020
            var split = value.split("-");
            var from = split[0].trim();
            var to = split[1].trim();
            condition = String.format("t.year >= %s AND t.year <= %s AND t.pageId IS NOT NULL", from, to);
        } else {
            // Invalid or unsupported format
            condition = "1=0"; // will return nothing
        }

        var matchedCoveredTexts = db.getDistinctTimesByCondition(condition, corpusId, 200);

        if (matchedCoveredTexts == null || matchedCoveredTexts.isEmpty()) {
            query.append(delimiter).append(value).append(delimiter).append(" ");
        } else {
            appendEnrichedNames(query, matchedCoveredTexts, value, delimiter, or);
            enrichedToken.setChildren(
                    matchedCoveredTexts.stream()
                            .map(n -> new EnrichedSearchToken(n, EnrichedSearchTokenType.TAXON))
                            .toList()
            );
        }

        return true;
    }

    private boolean handleTaxonomicCommand(@NotNull String token,
                                           @NotNull EnrichedSearchToken enrichedToken,
                                           StringBuilder query,
                                           String delimiter,
                                           String or) throws IOException {
        enrichedToken.setType(EnrichedSearchTokenType.TAXON_COMMAND);
        var command = token.substring(0, 3);
        var value = StringUtils.removeSpecialCharactersAtEdges(token.substring(3));
        enrichedToken.setValue(value);

        var speciesIds = jenaSparqlService.getSpeciesIdsOfUpperRank(
                getFullTaxonRankByCode(command.replace("::", "")), value, config.getSparqlMaxEnrichment());
        if(speciesIds.size() == config.getSparqlMaxEnrichment()) this.enrichedQueryIsCutOff = true;
        var names = jenaSparqlService.getAlternativeNamesOfTaxons(speciesIds);

        if (names == null || names.isEmpty()) {
            query.append(delimiter).append(value).append(delimiter).append(" ");
        } else {
            appendEnrichedNames(query, names, value, delimiter, or);
            enrichedToken.setChildren(names.stream()
                    .map(n -> new EnrichedSearchToken(n, EnrichedSearchTokenType.TAXON)).toList());
        }
        return true;
    }

    private boolean handleBasicTaxon(String cleanedToken,
                                     String originalToken,
                                     EnrichedSearchToken enrichedToken,
                                     StringBuilder query,
                                     String delimiter,
                                     String or) throws DatabaseOperationException, IOException {
        var taxonIds = db.getIdentifiableTaxonsByValue(cleanedToken.toLowerCase());
        if (taxonIds == null || taxonIds.isEmpty()) {
            return false;
        }

        enrichedToken.setType(EnrichedSearchTokenType.TAXON);
        var names = jenaSparqlService.getAlternativeNamesOfTaxons(taxonIds);
        if (names == null || names.isEmpty()) {
            query.append(originalToken).append(" ");
            return true;
        }

        appendEnrichedNames(query, names, originalToken.replaceAll("__", " "), delimiter, or);
        enrichedToken.setChildren(names.stream()
                .map(n -> new EnrichedSearchToken(n, EnrichedSearchTokenType.TAXON)).toList());

        return true;
    }

    private void appendEnrichedNames(StringBuilder query,
                                     List<String> names,
                                     String original,
                                     String delimiter,
                                     String or) {
        query.append(" ( ");
        if (!original.isEmpty()) query.append(original).append(or);
        query.append(delimiter)
                .append(String.join(or + delimiter, names.stream().filter(n -> !n.isBlank()).map(n -> n.replace("'", "") + delimiter).toList()))
                .append(" ) ");
    }

    public EnrichedSearchQuery withAll() {
        return this.withTaxonomy().withGeonames().withTimes();
    }

    public EnrichedSearchQuery withTaxonomy() {
        this.parseTaxonomy = true;
        return this;
    }

    public EnrichedSearchQuery withTimes() {
        this.parseTimes = true;
        return this;
    }

    public EnrichedSearchQuery withGeonames() {
        this.parseGeonames = true;
        return this;
    }
}
