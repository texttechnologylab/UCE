package org.texttechnologylab.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

import com.google.gson.Gson;
import com.pgvector.PGvector;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.corpus.Page;
import org.texttechnologylab.models.dto.RAGCompleteDto;
import org.texttechnologylab.models.dto.RAGEmbedDto;
import org.texttechnologylab.models.gbif.GbifOccurrence;
import org.texttechnologylab.models.rag.DocumentEmbedding;
import org.texttechnologylab.models.rag.RAGChatMessage;

import javax.print.Doc;

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
     * Queries our RAG webserver with a list of prefaced prompts to get the new message from our llm
     * @return
     */
    public String postNewRAGPrompt(List<RAGChatMessage> chatHistory){
        try {
            var httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .build();

            var url = config.getRAGWebserverBaseUrl() + "rag/complete";

            // Prepare workload
            var gson = new Gson();
            var params = new HashMap<String, Object>();

            params.put("model", config.getRAGModel());
            params.put("apiKey", "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"); // TODO: REMOVE THIS API KEY

            // Add the chat history
            var promptMessages = new ArrayList<HashMap<String, String>>();
            for(var chat:chatHistory.stream().sorted(Comparator.comparing(RAGChatMessage::getCreated)).toList()){
                var promptMessage = new HashMap<String, String>();
                promptMessage.put("role", chat.getRole().name().toString().toLowerCase());
                promptMessage.put("content", chat.getPrompt());
                promptMessages.add(promptMessage);
            }
            params.put("promptMessages", promptMessages);
            var jsonData = gson.toJson(params);

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
            var ragCompleteDto = gson.fromJson(responseBody, RAGCompleteDto.class);

            if(ragCompleteDto.getStatus() != 200){
                // TODO: Log this here?
                return null;
            }
            return ragCompleteDto.getMessage();
        } catch (Exception ex) {
            // TODO: Logging!
            return null;
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
            var embeddings = new ArrayList<DocumentEmbedding>();
            while(resultSet.next()){
                var embedding = new DocumentEmbedding(resultSet.getInt("beginn"), resultSet.getInt("endd"));
                embedding.setCoveredText(resultSet.getString("coveredtext"));
                embedding.setEmbedding(((PGvector)resultSet.getObject("embedding")).toArray());
                embedding.setDocument_id(resultSet.getLong("document_id"));
                embedding.setId(resultSet.getLong("id"));

                embeddings.add(embedding);
            }
            return embeddings;
        } catch (Exception ex){
            // TODO Log
            var xd = "";
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