package org.texttechnologylab.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bson.Document;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.models.corpus.MetadataTitleInfo;
import org.texttechnologylab.models.gbif.GbifOccurrence;
import org.texttechnologylab.models.util.HealthStatus;
import org.texttechnologylab.utils.SystemStatus;

import java.io.IOException;
import java.util.*;

public class GbifService {
    private CommonConfig config;
    private JenaSparqlService jenaSparqlService;

    public GbifService(JenaSparqlService jenaSparqlService) {
        try{
            config = new CommonConfig();
            this.jenaSparqlService = jenaSparqlService;

            SystemStatus.GbifServiceStatus = new HealthStatus(true, "", null);
        } catch (Exception ex){
            SystemStatus.GbifServiceStatus = new HealthStatus(false, "Couldn't init the service in constructor", ex);
        }
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
        var result = jenaSparqlService.executeCommand(command);
        if (result.isEmpty()) return -1;

        var gbifTaxonUrl = result.getFirst().getResource("object").toString();
        return Long.parseLong(Arrays.stream(gbifTaxonUrl.split("/")).toList().getLast());
    }

    /**
     * Screps potential occurrences from the official gbif database for a given biofidTaxonIdentifier
     *
     * @return
     */
    public List<GbifOccurrence> scrapeGbifOccurrence(long taxonId) throws IOException {
        var baseUrl = config.getGbifOccurrencesSearchUrl();
        var url = baseUrl.replace("{TAXON_ID}", Long.toString(taxonId));

        var data = Jsoup.connect(url).ignoreContentType(true).execute().body();
        var jsonData = Document.parse(data);
        var occurrences = new ArrayList<GbifOccurrence>();

        for (var result : (ArrayList<Document>) jsonData.get("results")) {
            var occurrence = new GbifOccurrence();
            occurrence.setGbifTaxonId(taxonId);
            occurrence.setOccurrenceId(Long.parseLong(result.get("key").toString()));
            occurrence.setImportedDate(DateTime.now());

            var media = (ArrayList<Document>) result.get("media");
            if (media != null && !media.isEmpty())
                occurrence.setImageUrl(Optional.ofNullable(media.get(0).get("identifier"))
                        .map(Object::toString)
                        .orElse(null));

            var dateIdentified = result.get("dateIdentified");
            if (dateIdentified != null) occurrence.setDateIdentified(DateTime.parse(dateIdentified.toString()));

            occurrence.setLatitude(Double.parseDouble(Optional.ofNullable(result.get("decimalLatitude"))
                    .map(Object::toString)
                    .orElse("-1000"))); // -1000 isnt a valid latitude.

            occurrence.setLongitude(Double.parseDouble(Optional.ofNullable(result.get("decimalLongitude"))
                    .map(Object::toString)
                    .orElse("-1000"))); // -1000 isnt a valid longitude.

            occurrence.setCountry(Optional.ofNullable(result.get("country"))
                    .map(Object::toString)
                    .orElse(null));

            occurrence.setRegion(Optional.ofNullable(result.get("verbatimLocality"))
                    .map(Object::toString)
                    .orElse(null));

            occurrences.add(occurrence);
        }
        return occurrences;
    }

}
