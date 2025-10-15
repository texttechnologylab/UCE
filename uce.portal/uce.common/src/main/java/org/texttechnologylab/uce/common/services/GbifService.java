package org.texttechnologylab.uce.common.services;

import org.bson.Document;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.texttechnologylab.uce.common.config.CommonConfig;
import org.texttechnologylab.uce.common.models.gbif.GbifOccurrence;
import org.texttechnologylab.uce.common.models.util.HealthStatus;
import org.texttechnologylab.uce.common.utils.SystemStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
