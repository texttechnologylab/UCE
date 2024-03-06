package org.texttechnologylab;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.texttechnologylab.config.SpringConfig;
import org.texttechnologylab.sparql.JenaSparqlFactory;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        System.out.println("Hello World!");
        testing();
    }

    /**
     * Just playing with the Jena API and its tutorial:
     * https://jena.apache.org/tutorials/rdf_api.html
     */
    private static void testing() {

        try {
            var context = new AnnotationConfigApplicationContext(SpringConfig.class);

            var search = new BiofidSearch(context,
                    "Biol in Paris und Montana",
                    new BiofidSearchLayer[]{BiofidSearchLayer.NAMED_ENTITIES, BiofidSearchLayer.TAXON});
            var result = search.initSearch();
        } catch (Exception ex) {
            var xd = "";
        }

        JenaSparqlFactory.initialize();

        var command = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX dwc: <http://rs.tdwg.org/dwc/terms/>\n" +
                "SELECT ?subject ?predicate ?object\n" +
                "WHERE {\n" +
                "  <https://www.biofid.de/bio-ontologies/gbif/11389112> ?predicate ?object .\n" +
                "}\n" +
                "LIMIT 100";
        var result = JenaSparqlFactory.executeCommand(command);
    }
}
