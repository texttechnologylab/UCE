package org.texttechnologylab.services;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.models.util.HealthStatus;
import org.texttechnologylab.utils.SystemStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JenaSparqlService {

    /**
     * Initializes the service like setting the default connection url. Service has to be initialized before it can be used.
     *
     * @return
     */
    public JenaSparqlService() {
        try (RDFConnection testConn = buildConnection()) {
            var test = biofidIdUrlToGbifTaxonId("");
            SystemStatus.JenaSparqlStatus = new HealthStatus(true, "", null);
        } catch (Exception ex) {
            SystemStatus.JenaSparqlStatus = new HealthStatus(false, "Couldn't connect a test conn to the sparql server.", ex);
        }
    }

    /**
     * Given a taxonid, it searches the sparql database for alternative names
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
    public List<String> getAlternativeNamesOfTaxons(List<String> biofidIds) {
        biofidIds = biofidIds.stream().distinct().toList();
        // We want all objects where the subjects fit any of the given ids and the predicate is either vascularName or scientificName
        // By doing so, we get more possible alternative names
        var command = "SELECT ?subject ?predicate ?object " +
                "WHERE {" +
                "  VALUES ?subject { {BIOFID_IDS} }" +
                "  ?subject ?predicate ?object . " +
                "  FILTER(?predicate IN (<http://rs.tdwg.org/dwc/terms/vernacularName>, <http://rs.tdwg.org/dwc/terms/scientificName>)) " +
                "}";
        command = command.replace("{BIOFID_IDS}", String.join("\n", biofidIds.stream().map(id -> "<" + id + ">").toList()));
        var result = executeCommand(command);
        var alternativeNames = new ArrayList<String>();
        for (var t : result) {
            var objectNode = t.get("object");
            // Check if the object is a literal
            if (objectNode.isLiteral()) {
                alternativeNames.add(objectNode.asLiteral().getString());
            }
            // otherwise, it's a resource
            else if (objectNode.isResource()) {
                alternativeNames.add(objectNode.asResource().getURI());
            }
        }
        return alternativeNames;
    }

    /**
     * Returns from e.g.: https://www.biofid.de/bio-ontologies/gbif/10428508 the taxon id that belongs to it.
     * We have that stored in our sparql database. Returns -1 if nothing was found.
     *
     * @return
     */
    public long biofidIdUrlToGbifTaxonId(String potentialBiofidId) {
        var command = "SELECT ?predicate ?object " +
                "WHERE {" +
                "  <{BIOFID_URL_ID}> <http://rs.tdwg.org/dwc/terms/taxonID> ?object ; " +
                "  . " +
                "}";
        command = command.replace("{BIOFID_URL_ID}", potentialBiofidId.trim());
        var result = executeCommand(command);
        if (result.isEmpty()) return -1;

        var gbifTaxonUrl = result.getFirst().getResource("object").toString();
        return Long.parseLong(Arrays.stream(gbifTaxonUrl.split("/")).toList().getLast());
    }

    private RDFConnection buildConnection() {
        var config = new CommonConfig();
        return RDFConnectionRemote.newBuilder()
                .destination(config.getSparqlHost())
                .queryEndpoint(config.getSparqlEndpoint())
                // Set a specific accept header; here, sparql-results+json (preferred) and text/tab-separated-values
                // The default is "application/sparql-results+json, application/sparql-results+xml;q=0.9, text/tab-separated-values;q=0.7, text/csv;q=0.5, application/json;q=0.2, application/xml;q=0.2, */*;q=0.1"
                .acceptHeaderSelectQuery("application/sparql-results+json, application/sparql-results+xml;q=0.9")
                .build();
    }

    /**
     * Executes a given command on the database and returns its List of QuerySolution
     *
     * @param command
     * @return
     */
    private ArrayList<QuerySolution> executeCommand(String command) {
        if(!SystemStatus.JenaSparqlStatus.isAlive()) {
            return null;
        }

        var querySolutions = new ArrayList<QuerySolution>();

        try (RDFConnection conn = buildConnection()) {
            conn.querySelect(command, querySolutions::add);
        }

        return querySolutions;
    }
}
