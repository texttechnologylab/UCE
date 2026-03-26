package org.texttechnologylab.uce.common.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.jsoup.HttpStatusException;
import org.texttechnologylab.uce.common.config.CommonConfig;
import org.texttechnologylab.uce.common.models.biofid.BiofidTaxon;
import org.texttechnologylab.uce.common.models.dto.rdf.RDFAskDto;
import org.texttechnologylab.uce.common.models.dto.rdf.RDFNodeDto;
import org.texttechnologylab.uce.common.models.dto.rdf.RDFRequestDto;
import org.texttechnologylab.uce.common.models.dto.rdf.RDFSelectQueryDto;
import org.texttechnologylab.uce.common.models.util.HealthStatus;
import org.texttechnologylab.uce.common.utils.QueryResultCache;
import org.texttechnologylab.uce.common.utils.RDFNodeDtoJsonDeserializer;
import org.texttechnologylab.uce.common.utils.StringUtils;
import org.texttechnologylab.uce.common.utils.SystemStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * UPDATE 12-2024: I completely replaced the org.apache.jena.rdfconnection imports and libraries as they were
 * HORRIBLE. They threw so many shitty errors under circumstances that were un-debuggable. I had error occur
 * in docker environments *only*, that made absolutely no sense, so fk it, I parse them by hand now with regular requests.
 * I wasted enough time to get a shit library working.
 */
public class JenaSparqlService {

    private final CommonConfig config = new CommonConfig();
    private final QueryResultCache queryCache = QueryResultCache.global(config);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private final ExecutorService sparqlExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final SparqlChunkParallelizer chunkParallelizer =
            new SparqlChunkParallelizer(sparqlExecutor, config.getSparqlConcurrentRequestsMax());
    private final int taxonBatchSize = config.getSparqlBatchSize();
    private static final int TAXON_TRAVERSAL_MAX_DEPTH = 4;
    private static final int TAXON_TRAVERSAL_MAX_NODES = 2000;
    private static final String DWC = "http://rs.tdwg.org/dwc/terms/";
    private static final String P_VERNACULAR = DWC + "vernacularName";
    private static final String P_CLEANED = DWC + "cleanedScientificName";
    private static final String P_SCIENTIFIC = DWC + "scientificName";
    private static final String P_SCIENTIFIC_AUTHORSHIP = DWC + "scientificNameAuthorship";
    private static final String P_ACCEPTED = DWC + "acceptedNameUsageID";
    private static final String P_PARENT = DWC + "parentNameUsageID";
    private static final String P_STATUS = DWC + "taxonomicStatus";
    private static final String P_TAXON_ID = DWC + "taxonID";
    private static final String P_TAXON_RANK = DWC + "taxonRank";

    /**
     * Initializes the service like setting the default connection url. Service has to be initialized before it can be used.
     *
     * @return
     */
    public JenaSparqlService() {
        TestConnection();
    }

    public void TestConnection() {
        try {
            if (isServerResponsive()) {
                SystemStatus.JenaSparqlStatus = new HealthStatus(true, "Connection successful.", null);
            } else {
                SystemStatus.JenaSparqlStatus = new HealthStatus(false, "Server not reachable, ask failed.", null);
                System.out.println("Unable to connect to the Fuseki Sparql database, hello returned false.");
            }
        } catch (Exception ex) {
            SystemStatus.JenaSparqlStatus = new HealthStatus(false, "Server returned an error, ask failed.", null);
        }
    }

    /**
     * Given a biofidUrl, returns a BiofidTaxon objects
     */
    public List<BiofidTaxon> queryBiofidTaxon(String biofidUrl) throws IOException, CloneNotSupportedException {
        if (!SystemStatus.JenaSparqlStatus.isAlive()) {
            return null;
        }

        var nodes = queryBySubject(biofidUrl);
        return BiofidTaxon.createFromRdfNodes(nodes);
    }

    /**
     * Given a graph database structure, this gets all triplets where the subject matches the given sub.
     * Example call: var test = queryBySubject("https://www.biofid.de/bio-ontologies/gbif/4356560");
     */
    public List<RDFNodeDto> queryBySubject(String sub) throws IOException {
        if (!SystemStatus.JenaSparqlStatus.isAlive()) {
            return new ArrayList<>();
        }

        var command = "SELECT * WHERE { " +
                "   <{SUB}> ?pred ?obj . " +
                "} " +
                "LIMIT 100";
        command = command.replace("{SUB}", sub);
        var result = executeCommand(command, RDFSelectQueryDto.class);
        if (result == null || result.getResults() == null || result.getResults().getBindings() == null)
            return new ArrayList<>();

        return result.getResults().getBindings().stream()
                .filter(n -> !n.getPredicate().getValue().contains("www.w3.org"))
                .toList();
    }

    /**
     * Given an upper taxonomic rank such as class, genus, phylum etc., fetches all species of that and returns their names.
     */
    public List<String> getSpeciesIdsOfUpperRank(String rank, String name, int limit) throws IOException {
        if (!SystemStatus.JenaSparqlStatus.isAlive()) {
            return new ArrayList<>();
        }

        // First off, we need to fetch the identifier of the objects for the rank with the given name
        // Once we have that, we can query all species that belong to that rank by its id
        var rankIds = getIdsOfTaxonRank(rank, name);
        return getSpeciesOfRank(rank, rankIds, limit);
    }

    /**
     * Given a taxonid, it searches the sparql database for alternative names, synonyms, subspecies and more and returns a list of names.
     * E.g. BioFID id: https://www.biofid.de/bio-ontologies/gbif/4299368
     * Example call:
     * <p>
     * PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
     * PREFIX dwc: <http://rs.tdwg.org/dwc/terms/>
     * <p>
     * SELECT ?subject ?predicate ?object
     * WHERE {
     * VALUES ?subject {
     * <https://www.biofid.de/bio-ontologies/gbif/4299368>
     * <https://www.biofid.de/bio-ontologies/gbif/2345678>
     * <https://www.biofid.de/bio-ontologies/gbif/3456789>
     * }
     * ?subject ?predicate ?object .
     * <p>
     * FILTER(?predicate IN (<http://rs.tdwg.org/dwc/terms/vernacularName>, <http://rs.tdwg.org/dwc/terms/scientificName>))
     * }
     *
     * @return
     */
    public List<String> getAlternativeNamesOfTaxons(List<String> biofidIds) throws IOException {
        var grouped = getAlternativeNamesGroupedOfTaxons(biofidIds);
        var flattened = new ArrayList<String>();
        for (var names : grouped.values()) {
            flattened.addAll(names);
        }
        return flattened;
    }

    /**
     * Returns grouped names for taxon enrichment, e.g. scientific names of seed ids,
     * scientific names of synonym ids, scientific names of subordinate taxa and all vernacular names.
     */
    public LinkedHashMap<String, List<String>> getAlternativeNamesGroupedOfTaxons(List<String> biofidIds) throws IOException {
        var detailed = getAlternativeNamesGroupedDetailedOfTaxons(biofidIds);
        var grouped = new LinkedHashMap<String, List<String>>();
        for (var entry : detailed.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) continue;
            grouped.put(entry.getKey(), entry.getValue().stream()
                    .map(GroupedTaxonChild::value)
                    .filter(v -> v != null && !v.isBlank())
                    .toList());
        }
        return grouped;
    }

    /**
     * Returns grouped enrichment entries including light metadata used by UI badges/tooltips.
     */
    public LinkedHashMap<String, List<GroupedTaxonChild>> getAlternativeNamesGroupedDetailedOfTaxons(List<String> biofidIds) throws IOException {
        if (!SystemStatus.JenaSparqlStatus.isAlive()) {
            return new LinkedHashMap<>();
        }

        var seedIds = new LinkedHashSet<>(biofidIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .toList());
        if (seedIds.isEmpty()) return new LinkedHashMap<>();

        var cacheKey = buildAlternativeNamesCacheKey(seedIds);
        try {
            var cachedJson = queryCache.getOrLoad(cacheKey, () -> {
                try {
                    var grouped = computeAlternativeNamesGroupedDetailedOfTaxons(seedIds);
                    return serializeGroupedChildren(grouped);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });
            return deserializeGroupedChildren(String.valueOf(cachedJson));
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }

    private LinkedHashMap<String, List<GroupedTaxonChild>> computeAlternativeNamesGroupedDetailedOfTaxons(Set<String> seedIds) throws IOException {
        var traversal = traverseTaxonGraph(seedIds);
        var factsById = queryTaxonFactsByIds(traversal.visitedIds);
        var preferredNameBySubject = new LinkedHashMap<String, String>();
        for (var entry : factsById.entrySet()) {
            var preferred = entry.getValue().getPrimaryScientificName();
            if (preferred != null && !preferred.isBlank()) {
                preferredNameBySubject.put(entry.getKey(), preferred);
            }
        }

        var grouped = new LinkedHashMap<String, LinkedHashMap<String, GroupedTaxonChild>>();
        grouped.put("SCIENTIFIC", new LinkedHashMap<>());
        grouped.put("SYNONYM", new LinkedHashMap<>());
        grouped.put("SUBORDINATE", new LinkedHashMap<>());
        grouped.put("VERNACULAR", new LinkedHashMap<>());

        for (var entry : factsById.entrySet()) {
            var subject = entry.getKey();
            var facts = entry.getValue();
            var ownerName = preferredNameBySubject.getOrDefault(subject, facts.getPrimaryScientificName());
            if (ownerName == null || ownerName.isBlank()) ownerName = "Unknown species";
            var rank = facts.getPrimaryRank();
            var rankLabel = formatRankLabel(rank);

            for (var vernacular : facts.vernacularNames) {
                if (vernacular == null || vernacular.isBlank()) continue;
                var child = new GroupedTaxonChild(vernacular, buildTooltipMeta("VERNACULAR", ownerName), "V", "neutral", "vernacular");
                grouped.get("VERNACULAR").putIfAbsent(vernacular, child);
            }

            var scientificNames = facts.getPreferredScientificNames();
            if (scientificNames.isEmpty()) continue;

            var isSynonym = traversal.synonymIds.contains(subject) || facts.hasSynonymStatus();
            var isSubordinate = traversal.subordinateIds.contains(subject);
            if (isSubordinate) {
                var parentName = facts.resolveParentName(preferredNameBySubject);
                var relationValue = parentName == null || parentName.isBlank() ? "Unknown parent" : parentName;
                var relationWithRank = buildTooltipMeta(rankLabel, relationValue);
                for (var name : scientificNames) {
                    if (name == null || name.isBlank()) continue;
                    grouped.get("SUBORDINATE").putIfAbsent(name,
                            new GroupedTaxonChild(name, relationWithRank, "SUB", "neutral", facts.getPrimaryRank()));
                }
            } else if (isSynonym) {
                var acceptedName = facts.resolveAcceptedName(preferredNameBySubject);
                var relationValue = acceptedName == null || acceptedName.isBlank()
                        ? "Unknown accepted species"
                        : acceptedName;
                for (var name : scientificNames) {
                    if (name == null || name.isBlank()) continue;
                    grouped.get("SYNONYM").putIfAbsent(name,
                            new GroupedTaxonChild(name, buildTooltipMeta("SYNONYM", relationValue), "SYN", "neutral", facts.getPrimaryRank()));
                }
            } else if (seedIds.contains(subject) || traversal.acceptedIds.contains(subject)) {
                var status = facts.getPrimaryStatus();
                var badgeTone = "neutral";
                var badgeText = "i";
                if (status != null && !status.isBlank()) {
                    if (status.contains("accepted")) {
                        badgeTone = "accepted";
                        badgeText = "A";
                    } else if (status.contains("doubtful")) {
                        badgeTone = "doubtful";
                        badgeText = "D";
                    }
                }
                var meta = buildTooltipMeta(rankLabel, formatStatusLabel(status));
                for (var name : scientificNames) {
                    if (name == null || name.isBlank()) continue;
                    grouped.get("SCIENTIFIC").putIfAbsent(name,
                            new GroupedTaxonChild(name, meta, badgeText, badgeTone, facts.getPrimaryRank()));
                }
            } else {
                for (var name : scientificNames) {
                    if (name == null || name.isBlank()) continue;
                    grouped.get("SYNONYM").putIfAbsent(name,
                            new GroupedTaxonChild(name, buildTooltipMeta("SYNONYM", "Unknown accepted species"), "SYN", "neutral", facts.getPrimaryRank()));
                }
            }
        }

        var result = new LinkedHashMap<String, List<GroupedTaxonChild>>();
        for (var entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            result.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
        }
        return result;
    }

    private String buildAlternativeNamesCacheKey(Set<String> seedIds) {
        var normalized = seedIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .distinct()
                .sorted()
                .collect(Collectors.joining("|"));
        return "sparql-alt-names-grouped:v4:"
                + config.getSparqlHost()
                + config.getSparqlEndpoint()
                + ":"
                + normalized;
    }

    private String serializeGroupedChildren(LinkedHashMap<String, List<GroupedTaxonChild>> grouped) {
        if (grouped == null || grouped.isEmpty()) return "{}";
        return GSON.toJson(grouped);
    }

    private LinkedHashMap<String, List<GroupedTaxonChild>> deserializeGroupedChildren(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        var type = new TypeToken<LinkedHashMap<String, List<GroupedTaxonChild>>>() {}.getType();
        LinkedHashMap<String, List<GroupedTaxonChild>> parsed = GSON.fromJson(json, type);
        return parsed == null ? new LinkedHashMap<>() : parsed;
    }

    private TraversalState traverseTaxonGraph(Set<String> seedIds) throws IOException {
        var state = new TraversalState();
        state.seedIds.addAll(seedIds);

        var frontier = new LinkedHashSet<>(seedIds);
        int depth = 0;
        while (!frontier.isEmpty() && depth <= TAXON_TRAVERSAL_MAX_DEPTH && state.visitedIds.size() < TAXON_TRAVERSAL_MAX_NODES) {
            frontier.removeAll(state.visitedIds);
            if (frontier.isEmpty()) break;

            state.visitedIds.addAll(frontier);
            var facts = queryTaxonFactsByIds(frontier);
            var nextFrontier = new LinkedHashSet<String>();

            for (var nodeFacts : facts.values()) {
                if (!nodeFacts.acceptedUsageIds.isEmpty()) {
                    state.acceptedIds.addAll(nodeFacts.acceptedUsageIds);
                    nextFrontier.addAll(nodeFacts.acceptedUsageIds);
                }
                if (nodeFacts.hasSynonymStatus()) {
                    state.synonymIds.add(nodeFacts.subject);
                }
            }

            var reverseSynonyms = querySubjectsByObject(P_ACCEPTED, frontier);
            state.synonymIds.addAll(reverseSynonyms);
            nextFrontier.addAll(reverseSynonyms);

            var children = querySubjectsByObject(P_PARENT, frontier);
            state.subordinateIds.addAll(children);
            nextFrontier.addAll(children);

            nextFrontier.removeAll(state.visitedIds);
            if (state.visitedIds.size() + nextFrontier.size() > TAXON_TRAVERSAL_MAX_NODES) {
                var capped = new LinkedHashSet<String>();
                int remaining = TAXON_TRAVERSAL_MAX_NODES - state.visitedIds.size();
                for (var id : nextFrontier) {
                    if (remaining-- <= 0) break;
                    capped.add(id);
                }
                nextFrontier = capped;
            }

            frontier = nextFrontier;
            depth++;
        }

        return state;
    }

    private LinkedHashMap<String, TaxonNodeFacts> queryTaxonFactsByIds(Set<String> ids) throws IOException {
        var bySubject = new LinkedHashMap<String, TaxonNodeFacts>();
        if (ids == null || ids.isEmpty()) return bySubject;

        var predicates = String.join(", ",
                "<" + P_VERNACULAR + ">",
                "<" + P_CLEANED + ">",
                "<" + P_SCIENTIFIC + ">",
                "<" + P_SCIENTIFIC_AUTHORSHIP + ">",
                "<" + P_ACCEPTED + ">",
                "<" + P_PARENT + ">",
                "<" + P_STATUS + ">",
                "<" + P_TAXON_ID + ">",
                "<" + P_TAXON_RANK + ">");

        var chunks = partition(ids, taxonBatchSize);
        var chunkResults = chunkParallelizer.runAll(chunks, chunk -> {
            var local = new LinkedHashMap<String, TaxonNodeFacts>();
            var command = "SELECT ?subject ?predicate ?object " +
                    "WHERE {" +
                    "  VALUES ?subject { {BIOFID_IDS} }" +
                    "  ?subject ?predicate ?object . " +
                    "  FILTER(?predicate IN ({PREDICATES})) " +
                    "}";
            command = command
                    .replace("{BIOFID_IDS}", String.join("\n", chunk.stream().map(id -> "<" + id + ">").toList()))
                    .replace("{PREDICATES}", predicates);

            var queryResult = executeCommand(command, RDFSelectQueryDto.class);
            if (queryResult == null || queryResult.getResults() == null || queryResult.getResults().getBindings() == null) return local;

            for (var binding : queryResult.getResults().getBindings()) {
                if (binding.getSubject() == null || binding.getPredicate() == null || binding.getObject() == null) continue;
                var subject = binding.getSubject().getValue();
                var predicate = binding.getPredicate().getValue();
                var object = binding.getObject().getValue();
                local.computeIfAbsent(subject, TaxonNodeFacts::new).add(predicate, object);
            }
            return local;
        });

        for (var partial : chunkResults) {
            for (var entry : partial.entrySet()) {
                var subject = entry.getKey();
                var facts = entry.getValue();
                var merged = bySubject.computeIfAbsent(subject, TaxonNodeFacts::new);
                merged.scientificNames.addAll(facts.scientificNames);
                merged.cleanedScientificNames.addAll(facts.cleanedScientificNames);
                merged.scientificAuthorships.addAll(facts.scientificAuthorships);
                merged.vernacularNames.addAll(facts.vernacularNames);
                merged.acceptedUsageIds.addAll(facts.acceptedUsageIds);
                merged.parentUsageIds.addAll(facts.parentUsageIds);
                merged.statuses.addAll(facts.statuses);
                merged.taxonIds.addAll(facts.taxonIds);
                merged.taxonRanks.addAll(facts.taxonRanks);
            }
        }

        for (var id : ids) bySubject.computeIfAbsent(id, TaxonNodeFacts::new);
        return bySubject;
    }

    private LinkedHashSet<String> querySubjectsByObject(String predicateUri, Set<String> objectIds) throws IOException {
        var subjects = new LinkedHashSet<String>();
        if (objectIds == null || objectIds.isEmpty()) return subjects;

        var chunks = partition(objectIds, taxonBatchSize);
        var chunkResults = chunkParallelizer.runAll(chunks, chunk -> {
            var localSubjects = new LinkedHashSet<String>();
            var command = "SELECT DISTINCT ?subject WHERE {" +
                    "  ?subject <{PREDICATE}> ?object . " +
                    "  VALUES ?object { {BIOFID_IDS} }" +
                    "}";
            command = command
                    .replace("{PREDICATE}", predicateUri)
                    .replace("{BIOFID_IDS}", String.join("\n", chunk.stream().map(id -> "<" + id + ">").toList()));

            var queryResult = executeCommand(command, RDFSelectQueryDto.class);
            if (queryResult == null || queryResult.getResults() == null || queryResult.getResults().getBindings() == null) return localSubjects;

            for (var binding : queryResult.getResults().getBindings()) {
                if (binding.getSubject() == null) continue;
                localSubjects.add(binding.getSubject().getValue());
            }
            return localSubjects;
        });

        for (var partial : chunkResults) {
            subjects.addAll(partial);
        }

        return subjects;
    }

    private List<List<String>> partition(Set<String> ids, int chunkSize) {
        var values = new ArrayList<>(ids);
        var chunks = new ArrayList<List<String>>();
        for (int i = 0; i < values.size(); i += chunkSize) {
            chunks.add(values.subList(i, Math.min(i + chunkSize, values.size())));
        }
        return chunks;
    }

    private String buildTooltipMeta(String label, String value) {
        return formatMetaLabel(label) + " | " + formatMetaValue(value);
    }

    private String formatRankLabel(String rank) {
        return formatMetaLabel(rank == null || rank.isBlank() ? "NAME" : rank);
    }

    private String formatStatusLabel(String status) {
        if (status == null || status.isBlank()) return "Unknown";
        var normalized = status.trim().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Arrays.stream(normalized.split("\\s+"))
                .filter(part -> part != null && !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(Collectors.joining(" "));
    }

    private String formatMetaLabel(String label) {
        if (label == null || label.isBlank()) return "NAME";
        return label.trim().replace('_', ' ').toUpperCase(Locale.ROOT);
    }

    private String formatMetaValue(String value) {
        if (value == null || value.isBlank()) return "Unknown";
        return value.trim();
    }

    private static class TraversalState {
        private final LinkedHashSet<String> seedIds = new LinkedHashSet<>();
        private final LinkedHashSet<String> visitedIds = new LinkedHashSet<>();
        private final LinkedHashSet<String> acceptedIds = new LinkedHashSet<>();
        private final LinkedHashSet<String> synonymIds = new LinkedHashSet<>();
        private final LinkedHashSet<String> subordinateIds = new LinkedHashSet<>();
    }

    private static class TaxonNodeFacts {
        private final String subject;
        private final LinkedHashSet<String> scientificNames = new LinkedHashSet<>();
        private final LinkedHashSet<String> cleanedScientificNames = new LinkedHashSet<>();
        private final LinkedHashSet<String> scientificAuthorships = new LinkedHashSet<>();
        private final LinkedHashSet<String> vernacularNames = new LinkedHashSet<>();
        private final LinkedHashSet<String> acceptedUsageIds = new LinkedHashSet<>();
        private final LinkedHashSet<String> parentUsageIds = new LinkedHashSet<>();
        private final LinkedHashSet<String> statuses = new LinkedHashSet<>();
        private final LinkedHashSet<String> taxonIds = new LinkedHashSet<>();
        private final LinkedHashSet<String> taxonRanks = new LinkedHashSet<>();

        private TaxonNodeFacts(String subject) {
            this.subject = subject;
        }

        private void add(String predicate, String value) {
            if (value == null || value.isBlank()) return;
            if (P_VERNACULAR.equals(predicate)) vernacularNames.add(value);
            else if (P_CLEANED.equals(predicate)) cleanedScientificNames.add(value);
            else if (P_SCIENTIFIC.equals(predicate)) scientificNames.add(value);
            else if (P_SCIENTIFIC_AUTHORSHIP.equals(predicate)) scientificAuthorships.add(value);
            else if (P_ACCEPTED.equals(predicate)) acceptedUsageIds.add(value);
            else if (P_PARENT.equals(predicate)) parentUsageIds.add(value);
            else if (P_STATUS.equals(predicate)) statuses.add(value.toLowerCase());
            else if (P_TAXON_ID.equals(predicate)) taxonIds.add(value);
            else if (P_TAXON_RANK.equals(predicate)) taxonRanks.add(value.toLowerCase());
        }

        private boolean hasSynonymStatus() {
            for (var status : statuses) {
                if (status.contains("synonym")) return true;
            }
            return false;
        }

        private LinkedHashSet<String> getPreferredScientificNames() {
            var names = new LinkedHashSet<String>();
            if (!scientificNames.isEmpty()) {
                names.addAll(scientificNames);
                return names;
            }

            if (!cleanedScientificNames.isEmpty() && !scientificAuthorships.isEmpty()) {
                for (var cleaned : cleanedScientificNames) {
                    for (var authorship : scientificAuthorships) {
                        if (authorship == null || authorship.isBlank()) continue;
                        names.add(cleaned + " " + authorship);
                    }
                }
                if (!names.isEmpty()) return names;
            }

            names.addAll(cleanedScientificNames);
            return names;
        }

        private String getPrimaryScientificName() {
            var names = getPreferredScientificNames();
            return names.isEmpty() ? null : names.iterator().next();
        }

        private String getPrimaryStatus() {
            return statuses.isEmpty() ? null : statuses.iterator().next();
        }

        private String getPrimaryRank() {
            return taxonRanks.isEmpty() ? null : taxonRanks.iterator().next();
        }

        private String resolveAcceptedName(Map<String, String> preferredNameBySubject) {
            for (var acceptedId : acceptedUsageIds) {
                var name = preferredNameBySubject.get(acceptedId);
                if (name != null && !name.isBlank()) return name;
            }
            return null;
        }

        private String resolveParentName(Map<String, String> preferredNameBySubject) {
            for (var parentId : parentUsageIds) {
                var name = preferredNameBySubject.get(parentId);
                if (name != null && !name.isBlank()) return name;
            }
            return null;
        }
    }

    public record GroupedTaxonChild(
            String value,
            String meta,
            String badgeText,
            String badgeTone,
            String rank
    ) {}

    /**
     * Returns from e.g.: https://www.biofid.de/bio-ontologies/gbif/10428508 the taxon id that belongs to it.
     * We have that stored in our sparql database. Returns -1 if nothing was found.
     */
    public long biofidIdUrlToGbifTaxonId(String potentialBiofidId) throws IOException {
        if (!SystemStatus.JenaSparqlStatus.isAlive()) {
            return -1;
        }

        var command = "SELECT ?predicate ?object " +
                "WHERE {" +
                "  <{BIOFID_URL_ID}> <http://rs.tdwg.org/dwc/terms/taxonID> ?object ; " +
                "  . " +
                "}";
        command = command.replace("{BIOFID_URL_ID}", potentialBiofidId.trim());
        var result = executeCommand(command, RDFSelectQueryDto.class);
        if (result == null || result.getResults() == null || result.getResults().getBindings() == null || result.getResults().getBindings().isEmpty())
            return -1;

        var gbifTaxonUrl = result.getResults().getBindings().getFirst().getObject().getValue();
        return Long.parseLong(Arrays.stream(gbifTaxonUrl.split("/")).toList().getLast());
    }

    /**
     * Executes a given command on the database and returns its List of QuerySolution
     *
     * @param command
     * @return
     */
    private <T extends RDFRequestDto> T executeCommand(String command, Class<T> clazz) throws IOException {
        // Put our prefixes into the command
        command = StringUtils.ConvertSparqlQuery(command);
        command = "PREFIX bio: <https://www.biofid.de/bio-ontologies/gbif/>\n" + command;
        var endPoint = config.getSparqlHost()
                + config.getSparqlEndpoint()
                + "?query="
                + URLEncoder.encode(command, StandardCharsets.UTF_8);
        var url = new URL(endPoint);
        var conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Parse the returned json
                try (var reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    var gson = new GsonBuilder()
                            .registerTypeAdapter(RDFNodeDto.class, new RDFNodeDtoJsonDeserializer())
                            .create();
                    return gson.fromJson(response.toString(), clazz);
                }
            } else {
                throw new HttpStatusException("Fuseki server returned error status: ", responseCode, endPoint);
            }
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Gets the ids of the desired rank by its name
     * Example:
     * SELECT distinct ?subject WHERE {
     *   ?s <http://rs.tdwg.org/dwc/terms/taxonRank> "genus"^^<xsd:string> .
     *   ?subject <http://rs.tdwg.org/dwc/terms/cleanedScientificName> "Corella"
     * } LIMIT 10
     */
    private List<String> getSpeciesOfRank(String rankName, List<String> ids, int limit) throws IOException {
        var command = "SELECT DISTINCT ?subject " +
                "WHERE { " +
                "    ?subject <http://rs.tdwg.org/dwc/terms/taxonRank> \"species\"^^<xsd:string> . " +
                "    ?subject <http://rs.tdwg.org/dwc/terms/{RANK}> ?rank . " +
                "    VALUES ?rank { " +
                "        {IDS}" +
                "    } " +
                "} " +
                "LIMIT {LIMIT}";
        command = command
                .replace("{RANK}", rankName)
                .replace("{LIMIT}", String.valueOf(limit))
                .replace("{IDS}", String.join("\n", ids.stream().map(i -> "<" + i + ">").toList()));
        var result = executeCommand(command, RDFSelectQueryDto.class);

        if (result == null || result.getResults() == null || result.getResults().getBindings() == null || result.getResults().getBindings().isEmpty())
            return new ArrayList<>();

        // If we fetched some results, we can now fetch species according to the ids of the rank
        var speciesIds = new ArrayList<String>();
        for(var binding:result.getResults().getBindings()){
            speciesIds.add(binding.getSubject().getValue());
        }
        return speciesIds;
    }

    /**
     * Gets the ids of the desired rank by its name
     * Example:
     * SELECT distinct ?subject WHERE {
     *   ?subject <http://rs.tdwg.org/dwc/terms/taxonRank> "genus"^^<xsd:string> .
     *   ?subject <http://rs.tdwg.org/dwc/terms/cleanedScientificName> "Corella" .
     * } LIMIT 10
     */
    public List<String> getIdsOfTaxonRank(String rank, String name) throws IOException {
        var rankCommand = "SELECT distinct ?subject WHERE {\n" +
                "  ?subject <http://rs.tdwg.org/dwc/terms/taxonRank> \"{RANK}\"^^<xsd:string> . " +
                "  ?subject <http://rs.tdwg.org/dwc/terms/cleanedScientificName> \"{NAME}\" . " +
                "} LIMIT 1";
        rankCommand = rankCommand.replace("{RANK}", rank).replace("{NAME}", name);
        var result = executeCommand(rankCommand, RDFSelectQueryDto.class);

        if (result == null || result.getResults() == null || result.getResults().getBindings() == null || result.getResults().getBindings().isEmpty())
            return new ArrayList<>();

        // If we fetched some results, we can now fetch species according to the ids of the rank
        var rankdIds = new ArrayList<String>();
        for(var binding:result.getResults().getBindings()){
            rankdIds.add(binding.getSubject().getValue());
        }
        return rankdIds;
    }

    private boolean isServerResponsive() throws IOException {
        String testQuery = "ASK WHERE { ?s ?p ?o }";
        var response = executeCommand(testQuery, RDFAskDto.class);
        if (response == null) return false;
        return response.isBool();
    }
}
