package org.texttechnologylab.uce.common.services;

import com.google.gson.GsonBuilder;
import org.jsoup.HttpStatusException;
import org.texttechnologylab.uce.common.config.CommonConfig;
import org.texttechnologylab.uce.common.models.biofid.BiofidTaxon;
import org.texttechnologylab.uce.common.models.dto.rdf.RDFAskDto;
import org.texttechnologylab.uce.common.models.dto.rdf.RDFNodeDto;
import org.texttechnologylab.uce.common.models.dto.rdf.RDFRequestDto;
import org.texttechnologylab.uce.common.models.dto.rdf.RDFSelectQueryDto;
import org.texttechnologylab.uce.common.models.util.HealthStatus;
import org.texttechnologylab.uce.common.utils.RDFNodeDtoJsonDeserializer;
import org.texttechnologylab.uce.common.utils.StringUtils;
import org.texttechnologylab.uce.common.utils.SystemStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * UPDATE 12-2024: I completely replaced the org.apache.jena.rdfconnection imports and libraries as they were
 * HORRIBLE. They threw so many shitty errors under circumstances that were un-debuggable. I had error occur
 * in docker environments *only*, that made absolutely no sense, so fk it, I parse them by hand now with regular requests.
 * I wasted enough time to get a shit library working.
 */
public class JenaSparqlService {

    private final CommonConfig config = new CommonConfig();

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
     * Given a list of accepted taxon URIs, return a list of URIs of synonyms that refer to them.
     */
    public List<String> getPossibleSynonymIdsOfTaxon(List<String> biofidUrls) throws IOException {
        var synonymIds = new HashSet<String>();

        for (var url : biofidUrls) {
            var query = """
            PREFIX dwc: <http://rs.tdwg.org/dwc/terms/>
            SELECT ?subject
            WHERE {
              ?subject dwc:acceptedNameUsageID <%s> .
              ?subject dwc:taxonomicStatus ?status .
              FILTER(lcase(str(?status)) = "synonym")
            }
            """.formatted(url);

            var result = executeCommand(query, RDFSelectQueryDto.class);
            if (result != null && result.getResults() != null && result.getResults().getBindings() != null) {
                for (var binding : result.getResults().getBindings()) {
                    synonymIds.add(binding.getSubject().getValue());
                }
            }
        }
        return new ArrayList<>(synonymIds);
    }

    public List<String> getSubordinateTaxonIds(List<String> biofidUrls) throws IOException {
        var subordinateIds = new HashSet<String>();

        for (var url : biofidUrls) {
            var query = """
            PREFIX dwc: <http://rs.tdwg.org/dwc/terms/>
            SELECT ?subject ?object
            WHERE {
              ?subject dwc:parentNameUsageID <%s> .
              ?subject dwc:taxonRank ?object .
              FILTER(lcase(str(?object)) IN ("subspecies", "varietas", "variety", "forma", "form"))
            }
            """.formatted(url);

            var result = executeCommand(query, RDFSelectQueryDto.class);
            if (result != null && result.getResults() != null && result.getResults().getBindings() != null) {
                for (var binding : result.getResults().getBindings()) {
                    subordinateIds.add(binding.getSubject().getValue());
                }
            }
        }

        return new ArrayList<>(subordinateIds);
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
        if (!SystemStatus.JenaSparqlStatus.isAlive()) {
            return new ArrayList<>();
        }

        biofidIds = biofidIds.stream().distinct().toList();
        var allIds = new HashSet<>(biofidIds);

        // Step 1: Add synonyms
        allIds.addAll(getPossibleSynonymIdsOfTaxon(biofidIds));

        // Step 2: Add subordinate taxa (subspecies, varieties, etc.)
        allIds.addAll(getSubordinateTaxonIds(biofidIds));

        // Step 3: Query for names of all taxon URIs
        var command = "SELECT ?subject ?predicate ?object " +
                      "WHERE {" +
                      "  VALUES ?subject { {BIOFID_IDS} }" +
                      "  ?subject ?predicate ?object . " +
                      "  FILTER(?predicate IN (<http://rs.tdwg.org/dwc/terms/vernacularName>, <http://rs.tdwg.org/dwc/terms/cleanedScientificName>)) " +
                      "}";
        command = command.replace("{BIOFID_IDS}", String.join("\n", allIds.stream().map(id -> "<" + id + ">").toList()));

        var result = executeCommand(command, RDFSelectQueryDto.class);
        var alternativeNames = new ArrayList<String>();
        if (result == null || result.getResults() == null || result.getResults().getBindings() == null)
            return alternativeNames;

        for (var t : result.getResults().getBindings()) {
            alternativeNames.add(t.getObject().getValue());
        }

        return alternativeNames;
    }

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
