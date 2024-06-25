package org.texttechnologylab.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.*;

import com.google.gson.Gson;
import com.pgvector.PGvector;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.corpus.PageTopicDistribution;
import org.texttechnologylab.models.corpus.TopicDistribution;
import org.texttechnologylab.models.dto.*;
import org.texttechnologylab.models.rag.DocumentChunkEmbedding;
import org.texttechnologylab.models.rag.DocumentEmbedding;
import org.texttechnologylab.models.rag.RAGChatMessage;

/**
 * Service class for RAG: Retrieval Augmented Generation
 */
public class RAGService {
    private PostgresqlDataInterface_Impl postgresqlDataInterfaceImpl = null;
    private Connection vectorDbConnection = null;
    private CommonConfig config;

    public RAGService(PostgresqlDataInterface_Impl postgresqlDataInterfaceImpl) {
        try {
            this.config = new CommonConfig();
            this.postgresqlDataInterfaceImpl = postgresqlDataInterfaceImpl;
            this.vectorDbConnection = setupVectorDbConnection();
        } catch (Exception ex) {
            // TODO: Logging
            System.out.println("Couldn't connect to vector database.");
        }
    }

    public <T extends TopicDistribution> T getTextTopicDistribution(Class<T> clazz, String text) {
        try {
            var httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .build();

            var url = config.getRAGWebserverBaseUrl() + "topic-modelling";

            // Prepare workload
            var gson = new Gson();
            var params = new HashMap<String, Object>();
            params.put("text", text);
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
            if (statusCode != 200) return null;
            var responseBody = response.body();
            var result = gson.fromJson(responseBody, TopicModellingDto.class);
            if (result.getStatus() != 200) {
                // TODO: Log this here?
                return null;
            }
            // Map the dto to our datastructure
            T topicDistribution = clazz.getDeclaredConstructor().newInstance();

            // This looks shit yikes. Happens.
            if(result.getRakeKeywords().size() > 2){
                topicDistribution.setRakeTopicOne(result.getRakeKeywords().get(0));
                topicDistribution.setRakeTopicTwo(result.getRakeKeywords().get(1));
                topicDistribution.setRakeTopicThree(result.getRakeKeywords().get(2));
            }
            if(result.getYakeKeywords().size() > 4){
                topicDistribution.setYakeTopicOne(result.getYakeKeywords().get(0));
                topicDistribution.setYakeTopicTwo(result.getYakeKeywords().get(1));
                topicDistribution.setYakeTopicThree(result.getYakeKeywords().get(2));
                topicDistribution.setYakeTopicFour(result.getYakeKeywords().get(3));
                topicDistribution.setYakeTopicFive(result.getYakeKeywords().get(4));
            }

            return topicDistribution;
        } catch (Exception ex) {
            // TODO: Logging!
            return null;
        }
    }


    /**
     * Given the corpusId, returns a fully renderd tsne plot from our python webserver as a string
     *
     * @param corpusId
     * @return
     */
    public String getCorpusTsnePlot(long corpusId) {
        try {
            var httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .build();

            var url = config.getRAGWebserverBaseUrl() + "plot/tsne";

            // Prepare workload
            var gson = new Gson();
            var params = new HashMap<String, Object>();

            // Get all documents of this corpus, loop through them, get the embeddings and
            // then send a request to our webserver.
            var corpusDocuments = postgresqlDataInterfaceImpl.getDocumentsByCorpusId(corpusId);
            var labels = new ArrayList<String>();
            var embeddings = new ArrayList<float[]>();
            for (var document : corpusDocuments) {
                if(document.getDocumentTopicDistribution() == null) continue;
                /* Probably best to average the embeddings of each document paragraph to one embedding
                 > Update: yes, let's go with it. We mean pool the multiple embeddings of a document if needed
                var pooledEmbedding = EmbeddingUtils.meanPooling(getDocumentChunkEmbeddingsOfDocument(document.getId())
                        .stream()
                        .map(DocumentChunkEmbedding::getEmbedding)
                        .toList());
                if(pooledEmbedding == null) continue;
                embeddings.add(pooledEmbedding);*/
                // > Update2: We now already have a single embedding representation of a doc
                var documentEmbedding = getDocumentEmbeddingOfDocument(document.getId());
                if(documentEmbedding == null) continue;
                embeddings.add(documentEmbedding.getTsne2d());

                // The labels are the topics of that document
                labels.add(document.getDocumentTopicDistribution().getYakeTopicOne());
            }
            params.put("labels", labels);
            params.put("embeddings", embeddings);
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
            if (statusCode != 200) return null;
            var responseBody = response.body();
            var plotTsneDto = gson.fromJson(responseBody, PlotTsneDto.class);

            if (plotTsneDto.getStatus() != 200) {
                // TODO: Log this here?
                return null;
            }

            return plotTsneDto.getPlot();
        } catch (Exception ex) {
            // TODO: Logging!
            return null;
        }
    }

    /**
     * Queries our RAG webserver which decides whether we should fetch new context or not.
     *
     * @return
     */
    public Integer postRAGContextNeeded(String userInput) {
        try {
            var httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .build();

            var url = config.getRAGWebserverBaseUrl() + "rag/context";

            // Prepare workload
            var gson = new Gson();
            var params = new HashMap<String, Object>();

            params.put("userInput", userInput);
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
            if (statusCode != 200) return null;
            var responseBody = response.body();
            var ragCompleteDto = gson.fromJson(responseBody, RAGCompleteDto.class);

            if (ragCompleteDto.getStatus() != 200) {
                // TODO: Log this here?
                return null;
            }
            return Integer.parseInt(ragCompleteDto.getMessage());
        } catch (Exception ex) {
            // TODO: Logging!
            return null;
        }
    }

    /**
     * Queries our RAG webserver with a list of prefaced prompts to get the new message from our llm
     *
     * @return
     */
    public String postNewRAGPrompt(List<RAGChatMessage> chatHistory) {
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
            for (var chat : chatHistory.stream().sorted(Comparator.comparing(RAGChatMessage::getCreated)).toList()) {
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
            if (statusCode != 200) return null;
            var responseBody = response.body();
            var ragCompleteDto = gson.fromJson(responseBody, RAGCompleteDto.class);

            if (ragCompleteDto.getStatus() != 200) {
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
     * Gets all embedding chunks of a document
     * @return
     */
    public ArrayList<DocumentChunkEmbedding> getDocumentChunkEmbeddingsOfDocument(long documentId) {
        try {
            var query = "SELECT * FROM documentchunkembeddings WHERE document_id = ?";
            var statement = vectorDbConnection.prepareStatement(query);
            statement.setLong(1, documentId);
            var resultSet = statement.executeQuery();
            return buildDocumentChunkEmbeddingsFromResultSet(resultSet);
        } catch (Exception ex) {
            // TODO Log
        }
        return new ArrayList<>();
    }

    /**
     * Gets the one embedding of a document. Can return NULL.
     * @return
     */
    public DocumentEmbedding getDocumentEmbeddingOfDocument(long documentId) {
        try {
            var query = "SELECT * FROM documentembeddings WHERE document_id = ?";
            var statement = vectorDbConnection.prepareStatement(query);
            statement.setLong(1, documentId);
            var resultSet = statement.executeQuery();
            // We return the first found docucment embedding as there should be only one.
            while (resultSet.next()) {
                var embedding = new DocumentEmbedding();
                embedding.setEmbedding(((PGvector) resultSet.getObject("embedding")).toArray());
                embedding.setTsne3d(resultSet.getObject("tsne3d") != null ? ((PGvector) resultSet.getObject("tsne3d")).toArray() : null);
                embedding.setTsne2d(resultSet.getObject("tsne2d") != null ? ((PGvector) resultSet.getObject("tsne2d")).toArray() : null);
                embedding.setDocument_id(resultSet.getLong("document_id"));
                embedding.setId(resultSet.getLong("id"));

                return embedding;
            }
        } catch (Exception ex) {
            System.err.println("Error getting a document embedding: " + documentId + " " + ex.getMessage());
            ex.printStackTrace();
            // TODO Log
        }
        return null;
    }

    /**
     * Gets the closest document embeddings from a given text controlled by the range variable
     *
     * @param text
     * @param range
     * @return
     */
    public List<DocumentChunkEmbedding> getClosestDocumentChunkEmbeddings(String text, int range) {
        try {
            var query = "SELECT * FROM documentchunkembeddings ORDER BY embedding <-> ? LIMIT ?";
            var statement = vectorDbConnection.prepareStatement(query);
            statement.setObject(1, new PGvector(getEmbeddingForText(text)));
            statement.setInt(2, range);
            var resultSet = statement.executeQuery();
            return buildDocumentChunkEmbeddingsFromResultSet(resultSet);
        } catch (Exception ex) {
            // TODO Log
            System.err.println("Error trying to get document chunks: " + ex.getMessage());
        }
        return null;
    }

    private ArrayList<DocumentChunkEmbedding> buildDocumentChunkEmbeddingsFromResultSet(ResultSet resultSet) throws SQLException {
        var embeddings = new ArrayList<DocumentChunkEmbedding>();
        while (resultSet.next()) {
            var embedding = new DocumentChunkEmbedding(resultSet.getInt("beginn"), resultSet.getInt("endd"));
            embedding.setCoveredText(resultSet.getString("coveredtext"));
            embedding.setEmbedding(((PGvector) resultSet.getObject("embedding")).toArray());
            embedding.setTsne3D(resultSet.getObject("tsne3d") != null ? ((PGvector) resultSet.getObject("tsne3d")).toArray() : null);
            embedding.setTsne2D(resultSet.getObject("tsne2d") != null ? ((PGvector) resultSet.getObject("tsne2d")).toArray() : null);
            embedding.setDocument_id(resultSet.getLong("document_id"));
            embedding.setId(resultSet.getLong("id"));

            embeddings.add(embedding);
        }
        return embeddings;
    }

    /**
     * Saves a document embedding.
     */
    public void saveDocumentEmbedding(DocumentEmbedding documentEmbedding) {
        String query = "INSERT INTO documentembeddings (document_id, embedding, tsne2d, tsne3d) VALUES (?, ?, ?, ?)";
        executeUpdate(query,
                documentEmbedding.getDocument_id(),
                new PGvector(documentEmbedding.getEmbedding()),
                new PGvector(documentEmbedding.getTsne2d()),
                new PGvector(documentEmbedding.getTsne3d()));
    }

    /**
     * Updates a document embedding.
     */
    public void updateDocumentEmbedding(DocumentEmbedding documentEmbedding) {
        String query = "UPDATE documentembeddings SET embedding = ?, tsne2d = ?, tsne3d = ? WHERE document_id = ?";
        executeUpdate(query,
                new PGvector(documentEmbedding.getEmbedding()),
                new PGvector(documentEmbedding.getTsne2d()),
                new PGvector(documentEmbedding.getTsne3d()),
                documentEmbedding.getDocument_id());
    }

    /**
     * Saves a document chunk embedding.
     */
    public void saveDocumentChunkEmbedding(DocumentChunkEmbedding documentChunkEmbedding) {
        String query = "INSERT INTO documentchunkembeddings (document_id, embedding, coveredtext, beginn, endd, tsne2d, tsne3d) VALUES (?, ?, ?, ?, ?, ?, ?)";
        executeUpdate(query,
                documentChunkEmbedding.getDocument_id(),
                new PGvector(documentChunkEmbedding.getEmbedding()),
                documentChunkEmbedding.getCoveredText(),
                documentChunkEmbedding.getBegin(),
                documentChunkEmbedding.getEnd(),
                new PGvector(documentChunkEmbedding.getTsne2D()),
                new PGvector(documentChunkEmbedding.getTsne3D()));
    }

    /**
     * Updates a document chunk embedding.
     */
    public void updateDocumentChunkEmbedding(DocumentChunkEmbedding documentChunkEmbedding) {
        String query = "UPDATE documentchunkembeddings SET embedding = ?, coveredtext = ?, beginn = ?, endd = ?, tsne2d = ?, tsne3d = ? WHERE document_id = ?";
        executeUpdate(query,
                new PGvector(documentChunkEmbedding.getEmbedding()),
                documentChunkEmbedding.getCoveredText(),
                documentChunkEmbedding.getBegin(),
                documentChunkEmbedding.getEnd(),
                new PGvector(documentChunkEmbedding.getTsne2D()),
                new PGvector(documentChunkEmbedding.getTsne3D()),
                documentChunkEmbedding.getDocument_id());
    }

    /**
     * Executes an update on the RAG database
     * @param query
     * @param params
     */
    private void executeUpdate(String query, Object... params) {
        try {
            var statement = vectorDbConnection.prepareStatement(query);
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof PGvector) {
                    statement.setObject(i + 1, params[i]);
                } else if (params[i] instanceof String) {
                    statement.setString(i + 1, (String) params[i]);
                } else if (params[i] instanceof Integer) {
                    statement.setInt(i + 1, (Integer) params[i]);
                } else if (params[i] instanceof Long) {
                    statement.setLong(i + 1, (Long) params[i]);
                }
                // Add other types as needed
            }
            statement.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: Logging
        }
    }

    /**
     * Gets a single DocumentEmbedding for a whole document.
     * @param document
     * @return
     */
    public DocumentEmbedding getCompleteEmbeddingFromDocument(Document document){
        var documentEmbedding = new DocumentEmbedding();
        documentEmbedding.setDocument_id(document.getId());
        documentEmbedding.setEmbedding(getEmbeddingForText(document.getFullText()));
        return documentEmbedding;
    }

    /**
     * Gets the complete and embedded lists of DocumentChunkEmbeddings for a single document
     *
     * @param document
     */
    public List<DocumentChunkEmbedding> getCompleteEmbeddingChunksFromDocument(Document document) {
        // We also make an embedding from the title
        var emptyEmbeddings = getEmptyEmbeddingChunksFromText(document.getDocumentTitle() + " " + document.getFullText(), 900);
        for (var empty : emptyEmbeddings) {
            var embeddings = getEmbeddingForText(empty.getCoveredText());
            empty.setEmbedding(embeddings);
            empty.setDocument_id(document.getId());
        }
        return emptyEmbeddings;
    }

    /**
     * A function that reduces the vector embeddings into 2D and 3D embeddings through tsne on our webserver.
     */
    public EmbeddingReduceDto getEmbeddingDimensionReductions(List<float[]> embeddings) {
        try {
            var httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .build();

            var url = config.getRAGWebserverBaseUrl() + "embed/reduce";

            // Prepare workload
            var gson = new Gson();
            var params = new HashMap<String, Object>();
            params.put("embeddings", embeddings);
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
            if (statusCode != 200) return null;
            var responseBody = response.body();
            var reductionDto = gson.fromJson(responseBody, EmbeddingReduceDto.class);

            if (reductionDto.getStatus() != 200) {
                // TODO: Log this here?
                return null;
            }
            return reductionDto;
        } catch (Exception ex) {
            // TODO: Logging!
            return null;
        }
    }

    /**
     * A function that fetches the vector embeddings of a given text through our python webserver
     */
    public float[] getEmbeddingForText(String text) {
        try {
            var httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .build();

            var url = config.getRAGWebserverBaseUrl() + "embed";

            // Prepare workload
            var gson = new Gson();
            var params = new HashMap<String, Object>();
            params.put("text", text);
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
            if (statusCode != 200) return null;
            var responseBody = response.body();
            var ragEmbedDto = gson.fromJson(responseBody, RAGEmbedDto.class);

            if (ragEmbedDto.getStatus() != 200) {
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
     * Gets a list of empty DocumentChunkEmbeddings with proper text splitting
     *
     * @param text
     * @param chunkSize Good size would be 1.200 for example
     * @return
     */
    public List<DocumentChunkEmbedding> getEmptyEmbeddingChunksFromText(String text, int chunkSize) {
        // Calculate the number of chunks needed
        // We want a cleaned single text without linebreaks or whatnot.
        text = text.replaceAll("\\s+", " ");
        int numChunks = (int) Math.ceil((double) text.length() / chunkSize);
        var emptyEmbeddings = new ArrayList<DocumentChunkEmbedding>();

        // Split the input text into chunks
        for (int i = 0; i < numChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, text.length());
            var chunk = text.substring(start, end);
            var emptyEmbedding = new DocumentChunkEmbedding(start, end);
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
