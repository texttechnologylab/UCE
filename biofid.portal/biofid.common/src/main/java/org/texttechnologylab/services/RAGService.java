package org.texttechnologylab.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;
import com.pgvector.PGvector;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.dto.RAGEmbedDto;
import org.texttechnologylab.models.gbif.GbifOccurrence;
import org.texttechnologylab.models.rag.DocumentEmbedding;

/**
 * Service class for RAG: Retrieval Augmented Generation
 */
public class RAGService {
    private DatabaseService databaseService = null;
    private Connection vectorDbConnection = null;
    private CommonConfig config;

    public RAGService(DatabaseService databaseService){
        try{
            this.config = new CommonConfig();
            this.databaseService = databaseService;
            this.vectorDbConnection = setupVectorDbConnection();
        } catch (Exception ex){
            // TODO: Logging
            System.out.println("Couldn't connect to vector database.");
        }
    }

    /**
     * Gets the closest document embeddings from a given text controlled by the range variable
     * @param text
     * @param range
     * @return
     */
    public List<DocumentEmbedding> getClosestDocumentEmbeddings(String text, int range){
        try {
            var query = "SELECT * FROM documentembeddings ORDER BY embedding <-> ? LIMIT ?";
            var statement = vectorDbConnection.prepareStatement(query);
            statement.setObject(1, new PGvector(getEmbeddingForText(text)));
            statement.setInt(2, range);
            var resultSet = statement.executeQuery();
            while(resultSet.next()){
                var coveredText = resultSet.getString("coveredtext");
                // TODO: Build the models here and finish this method.
            }
        } catch (Exception ex){
            var xd = "";
            // TODO Log
        }
        return null;
    }

    /**
     * Stores a signle document embedding
     */
    public void saveDocumentEmbedding(DocumentEmbedding documentEmbedding){
        try{
            var query = "INSERT INTO documentembeddings (document_id, embedding, coveredtext, beginn, endd) VALUES (?, ?, ?, ?, ?)";
            var insertStatement = vectorDbConnection.prepareStatement(query);
            insertStatement.setLong(1, documentEmbedding.getDocument_id());
            insertStatement.setObject(2, new PGvector(documentEmbedding.getEmbedding()));
            insertStatement.setString(3, documentEmbedding.getCoveredText());
            insertStatement.setInt(4, documentEmbedding.getBegin());
            insertStatement.setInt(5, documentEmbedding.getEnd());

            insertStatement.executeUpdate();
        } catch (Exception ex){
            ex.printStackTrace();
            // TODO: Logging
        }
    }

    /**
     * Gets the complete and embedded lists of DocumentEmbeddings for a single document
     * @param document
     */
    public List<DocumentEmbedding> getCompleteEmbeddingsFromDocument(Document document){
        var emptyEmbeddings = getEmptyEmbeddingsFromText(document.getFullText(), 1200);
        for(var empty:emptyEmbeddings){
            var embeddings = getEmbeddingForText(empty.getCoveredText());
            empty.setEmbedding(embeddings);
            empty.setDocument_id(document.getId());
        }
        return emptyEmbeddings;
    }

    /**
     * A function that fetches the vector embeddings of a given text through our python webserver
     */
    public float[] getEmbeddingForText(String text){
        try {
            var httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .build();

            var url = config.getRAGWebserverBaseUrl() + "embed";

            // Prepare workload
            var gson = new Gson();
            var params = new HashMap<String, Object>();
            params.put("text", text);
            var jsonData = gson.toJson(params); // Example JSON data

            // Create request
            var request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                    .build();
            // Send request and get response
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            var statusCode = response.statusCode();
            if(statusCode != 200) return null;
            var responseBody = response.body();
            var ragEmbedDto = gson.fromJson(responseBody, RAGEmbedDto.class);

            if(ragEmbedDto.getStatus() != 200){
                // TODO: Log this here?
                return null;
            }
            return ragEmbedDto.getMessage();
        } catch (Exception ex) {
            // TODO: Logging!
            return null;
        }
    }

    /**
     * Gets a list of empty DocumetnEmbeddings with proper text splitting
     * @param text
     * @param chunkSize Good size would be 1.200 for example
     * @return
     */
    public List<DocumentEmbedding> getEmptyEmbeddingsFromText(String text, int chunkSize){
        // Calculate the number of chunks needed
        // We want a cleaned single text without linebreaks or whatnot.
        text = text.replaceAll("\\s+", " ");
        int numChunks = (int) Math.ceil((double) text.length() / chunkSize);
        var emptyEmbeddings = new ArrayList<DocumentEmbedding>();

        // Split the input text into chunks
        for (int i = 0; i < numChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, text.length());
            var chunk = text.substring(start, end);
            var emptyEmbedding = new DocumentEmbedding(start, end);
            emptyEmbedding.setCoveredText(chunk);
            emptyEmbeddings.add(emptyEmbedding);
        }
        return emptyEmbeddings;
    }

    /**
     * We are using postgresql vector extension, which won't work with hibernate. Hence we open a different connection
     * solely for the RAG service.
     */
    private Connection setupVectorDbConnection() throws ClassNotFoundException, SQLException {
        Class.forName(config.getPostgresqlProperty("connection.driver_class"));
        var connection = DriverManager.getConnection(config.getPostgresqlProperty("hibernate.connection.url"),
                config.getPostgresqlProperty("hibernate.connection.username"),
                config.getPostgresqlProperty("hibernate.connection.password"));

        // After we have the connection, we setup some vector extension requirements.
        var setupStmt = connection.createStatement();
        setupStmt.executeUpdate("CREATE EXTENSION IF NOT EXISTS vector");
        PGvector.addVectorType(connection);

        return connection;
    }

}
