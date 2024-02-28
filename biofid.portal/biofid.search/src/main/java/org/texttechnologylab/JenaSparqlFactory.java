package org.texttechnologylab;

import io.micrometer.common.lang.Nullable;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public final class JenaSparqlFactory {

    private static boolean isInitialized = false;
    private static RDFConnection defaultConnection;

    /**
     * Initializes the factory like setting the default connection url. Factory has to be initialized before it can be used.
     * @return
     */
    public static boolean initialize(){
        try{
            // TODO: Add there confg connectionstring loading and more.
            defaultConnection = RDFConnectionRemote.newBuilder()
                    .destination("http://localhost:3030/")
                    .queryEndpoint("biofid-search/sparql")
                    // Set a specific accept header; here, sparql-results+json (preferred) and text/tab-separated-values
                    // The default is "application/sparql-results+json, application/sparql-results+xml;q=0.9, text/tab-separated-values;q=0.7, text/csv;q=0.5, application/json;q=0.2, application/xml;q=0.2, */*;q=0.1"
                    .acceptHeaderSelectQuery("application/sparql-results+json, application/sparql-results+xml;q=0.9")
                    .build();

            isInitialized = true;
            return isInitialized;
        } catch(Exception ex){

        }
        return false;
    }

    /**
     * Executes a given command on the database and returns its List of QuerySolution
     * @param command
     * @return
     */
    public static ArrayList<QuerySolution> executeCommand(String command){
        if(!isInitialized)
            throw new ExceptionInInitializerError("The factory wasn't correctly initialized before " +
                    "trying to execute commands - call Factory.initialize(); first.");

        var querySolutions = new ArrayList<QuerySolution>();

        // Handles the closing of the ressource as well.
        try ( RDFConnection conn = defaultConnection ) {
            //var subject = qs.getResource("pred");
            conn.querySelect(command, querySolutions::add);
        } catch (Exception ex){
            System.out.println(ex.getMessage());
        }

        return querySolutions;
    }

}
