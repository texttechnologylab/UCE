package org.texttechnologylab;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.VCARD;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;

import java.io.InputStream;

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

        JenaSparqlFactory.initialize();

        var command = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX dwc: <http://rs.tdwg.org/dwc/terms/>\n" +
                "\n" +
                "SELECT ?scientificName ?authorship ?taxonRank ?taxonomicStatus ?kingdom\n" +
                "WHERE {\n" +
                "\t?subject rdf:type ?type ;\n" +
                "  \tdwc:scientificName ?scientificName ;\n" +
                "    dwc:scientificNameAuthorship ?authorship ;\n" +
                "    dwc:taxonRank ?taxonRank ;\n" +
                "    dwc:kingdom ?kingdom ;\n" +
                "    dwc:taxonomicStatus ?taxonomicStatus .\n" +
                "  \n" +
                "  FILTER(CONTAINS(str(?scientificName), \"para\"))\n" +
                "} LIMIT 100";
        var result = JenaSparqlFactory.executeCommand(command);
    }
}
