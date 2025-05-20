package org.texttechnologylab.models.search;

import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.models.corpus.GeoName;
import org.texttechnologylab.models.corpus.GeoNameFeatureClass;
import org.texttechnologylab.models.dto.LocationDto;
import org.texttechnologylab.models.viewModels.CorpusViewModel;
import org.texttechnologylab.services.JenaSparqlService;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.utils.StringUtils;
import org.texttechnologylab.utils.SystemStatus;

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
    public static final String[] TAX_RANKS = {"G::", "F::", "O::", "C::", "P::", "K::"};
    public static final String[] LOCATION_COMMANDS = {"LOC::", "R::"};
    public static final String[] ENRICHMENT_COMMANDS = ArrayUtils.addAll(TAX_RANKS, LOCATION_COMMANDS);

    private final PostgresqlDataInterface_Impl db;
    private final JenaSparqlService jenaSparqlService;

    @Getter
    private final String originalQuery;
    @Getter
    private String enrichedQuery;
    @Getter
    private List<EnrichedSearchToken> enrichedSearchTokens;
    private boolean parseTaxonomy;
    private boolean parseGeonames;

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
        if (command.equals("R::")) {
            var locationDto = parseLocationRadiusCommand(value);
            // TODO: Continue here
        } else if (command.equals("LOC::")) {
            // The syntax should be  LOC::<FEATURE_CLASS>.<FEATURE_CODE>, so e.g.: LOC::A.ADMS
            var split = value.split("\\.");
            var featureClass = split[0];
            var featureCode = "";
            if (split.length > 1) featureCode = split[1];

            var geoNames = db.getDistinctGeonamesNamesByFeatureCode(GeoNameFeatureClass.valueOf(featureClass), featureCode, corpusId, 100);

            if (geoNames.isEmpty()) {
                query.append(delimiter).append(value).append(delimiter).append(" ");
            } else {
                appendEnrichedNames(query, geoNames, "", delimiter, or);
                enrichedToken.setChildren(geoNames.stream()
                        .map(n -> new EnrichedSearchToken(n, EnrichedSearchTokenType.LOCATION)).toList());
            }
        }

        return true;
    }

    public static LocationDto parseLocationRadiusCommand(@NotNull String input) {
        if (input.startsWith("R::")) input = input.replace("R::", "");
        return LocationDto.fromCommandString(input);
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
                StringUtils.getFullTaxonRankByCode(command.replace("::", "")), value);
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
        var potentialTaxons = db.getIdentifiableTaxonsByValues(List.of(cleanedToken.toLowerCase()));
        if (potentialTaxons == null || potentialTaxons.isEmpty()) {
            //query.append(originalToken.replaceAll("__", " ")).append(" ");
            return false;
        }

        enrichedToken.setType(EnrichedSearchTokenType.TAXON);
        var taxonIds = new ArrayList<String>();
        for (var taxon : potentialTaxons) {
            if (taxon.getIdentifier().contains("|") || taxon.getIdentifier().contains(" ")) {
                taxonIds.addAll(taxon.getIdentifierAsList());
            } else {
                taxonIds.add(taxon.getIdentifier().trim());
            }
        }

        var names = jenaSparqlService.getAlternativeNamesOfTaxons(taxonIds);
        if (names == null || names.isEmpty()) {
            query.append(originalToken).append(" ");
            return false;
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
        if (!original.isEmpty())query.append(original).append(or);
        query.append(delimiter)
                .append(String.join(or + delimiter, names.stream().map(n -> n.replace("'", "") + delimiter).toList()))
                .append(" ) ");
    }

    public EnrichedSearchQuery withAll() {
        return this.withTaxonomy().withGeonames();
    }

    public EnrichedSearchQuery withTaxonomy() {
        this.parseTaxonomy = true;
        return this;
    }

    public EnrichedSearchQuery withGeonames() {
        this.parseGeonames = true;
        return this;
    }
}
