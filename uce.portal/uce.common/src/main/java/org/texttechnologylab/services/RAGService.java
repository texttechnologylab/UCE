package org.texttechnologylab.services;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.Duration;
import java.util.*;

import com.google.gson.Gson;
import com.pgvector.PGvector;
import org.jsoup.HttpStatusException;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.corpus.KeywordDistribution;
import org.texttechnologylab.models.dto.*;
import org.texttechnologylab.models.rag.DocumentChunkEmbedding;
import org.texttechnologylab.models.rag.DocumentEmbedding;
import org.texttechnologylab.models.rag.RAGChatMessage;
import org.texttechnologylab.models.util.HealthStatus;
import org.texttechnologylab.utils.SystemStatus;

/**
 * Service class for RAG: Retrieval Augmented Generation
 */
public class RAGService {
    private PostgresqlDataInterface_Impl postgresqlDataInterfaceImpl = null;
    private Connection vectorDbConnection = null;
    private CommonConfig config;

    public RAGService(PostgresqlDataInterface_Impl postgresqlDataInterfaceImpl) {
        this.postgresqlDataInterfaceImpl = postgresqlDataInterfaceImpl;
        TestConnection();
    }

    public void TestConnection(){
        try {
            this.config = new CommonConfig();
            this.vectorDbConnection = setupVectorDbConnection();

            var test = ExceptionUtils.tryCatchLog(
                    () -> getEmbeddingForText("This is an embedding test."),
                    (ex) -> SystemStatus.RagServiceStatus = new HealthStatus(false, "Embedding the text failed", ex));

            if(test != null){
                SystemStatus.RagServiceStatus = new HealthStatus(true, "", null);
            }
        } catch (Exception ex) {
            SystemStatus.RagServiceStatus = new HealthStatus(false, "Couldn't connect to the vector database.", ex);
        }
    }

    public <T extends KeywordDistribution> T getTextKeywordDistribution(Class<T> clazz, String text) throws
            URISyntaxException,
            IOException,
            InterruptedException,
            NoSuchMethodException,
            InvocationTargetException,
            InstantiationException,
            IllegalAccessException {
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
        if (statusCode != 200) throw new HttpStatusException("Request returned invalid status code: " + statusCode, statusCode, url);

        var responseBody = response.body();
        var result = gson.fromJson(responseBody, TopicModellingDto.class);
        if (result.getStatus() != 200) throw new HttpStatusException(
                "Webservice replied with an internally wrong status code, something went wrong there: " + result.getStatus(), statusCode, url);

        // Map the dto to our datastructures
        T KeywordDistribution = clazz.getDeclaredConstructor().newInstance();

        // This looks shit yikes. Happens.
        if (result.getRakeKeywords().size() > 2) {
            KeywordDistribution.setRakeTopicOne(result.getRakeKeywords().get(0));
            KeywordDistribution.setRakeTopicTwo(result.getRakeKeywords().get(1));
            KeywordDistribution.setRakeTopicThree(result.getRakeKeywords().get(2));
        }
        if (result.getYakeKeywords().size() > 4) {
            KeywordDistribution.setYakeTopicOne(result.getYakeKeywords().get(0));
            KeywordDistribution.setYakeTopicTwo(result.getYakeKeywords().get(1));
            KeywordDistribution.setYakeTopicThree(result.getYakeKeywords().get(2));
            KeywordDistribution.setYakeTopicFour(result.getYakeKeywords().get(3));
            KeywordDistribution.setYakeTopicFive(result.getYakeKeywords().get(4));
        }

        return KeywordDistribution;
    }


    /**
     * Given the corpusId, returns a fully renderd tsne plot from our python webserver as a string
     *
     * @param corpusId
     * @return
     */
    public String getCorpusTsnePlot(long corpusId) throws DatabaseOperationException, URISyntaxException, IOException, InterruptedException, SQLException {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        var url = config.getRAGWebserverBaseUrl() + "plot/tsne";

        // Prepare workload
        var gson = new Gson();
        var params = new HashMap<String, Object>();

        // Get all documents of this corpus, loop through them, get the embeddings and
        // then send a request to our webserver.
        var corpusDocuments = postgresqlDataInterfaceImpl.getDocumentsByCorpusId(corpusId, 0, 9999999);
        var labels = new ArrayList<String>();
        var embeddings = new ArrayList<float[]>();
        for (var document : corpusDocuments) {
            if (document.getDocumentKeywordDistribution() == null) continue;
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
            if (documentEmbedding == null) continue;
            embeddings.add(documentEmbedding.getTsne2d());

            // The labels are the topics of that document
            labels.add(document.getDocumentKeywordDistribution().getYakeTopicOne());
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
        if (statusCode != 200) throw new HttpStatusException("Request returned invalid status code: " + statusCode, statusCode, url);

        var responseBody = response.body();
        var plotTsneDto = gson.fromJson(responseBody, PlotTsneDto.class);
        if (plotTsneDto.getStatus() != 200) throw new HttpStatusException(
                "Webservice replied with an internally wrong status code, something went wrong there: " + plotTsneDto.getStatus(), statusCode, url);

        return plotTsneDto.getPlot();
    }

    /**
     * Queries our RAG webserver which decides whether we should fetch new context or not.
     *
     * @return
     */
    public Integer postRAGContextNeeded(String userInput) throws URISyntaxException, IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        var url = config.getRAGWebserverBaseUrl() + "rag/context";

        // Prepare workload
        var gson = new Gson();
        var params = new HashMap<String, Object>();

        // Its better to end each sentence with a ? for the webservice.
        if(!userInput.endsWith("?")) userInput = userInput + "?";
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
        if (statusCode != 200) throw new HttpStatusException("Request returned invalid status code: " + statusCode, statusCode, url);

        var responseBody = response.body();
        var ragCompleteDto = gson.fromJson(responseBody, RAGCompleteDto.class);
        if (ragCompleteDto.getStatus() != 200) throw new HttpStatusException(
                "Webservice replied with an internally wrong status code, something went wrong there: " + ragCompleteDto.getStatus(), statusCode, url);

        return Integer.parseInt(ragCompleteDto.getMessage());
    }

    /**
     * Queries our RAG webserver with a list of prefaced prompts to get the new message from our llm
     *
     * @return
     */
    public String postNewRAGPrompt(List<RAGChatMessage> chatHistory) throws URISyntaxException, IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        var url = config.getRAGWebserverBaseUrl() + "rag/complete";
        var config = new CommonConfig();

        // Prepare workload
        var gson = new Gson();
        var params = new HashMap<String, Object>();

        params.put("model", config.getRAGModel());
        params.put("apiKey", config.getRagOpenAIApiKey());

        // Add the chat history
        var promptMessages = new ArrayList<HashMap<String, String>>();
        for (var chat : chatHistory.stream().sorted(Comparator.comparing(RAGChatMessage::getCreated)).toList()) {
            var promptMessage = new HashMap<String, String>();
            promptMessage.put("role", chat.getRole().name().toLowerCase());
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
        if (statusCode != 200) throw new HttpStatusException("Request returned invalid status code: " + statusCode, statusCode, url);

        var responseBody = response.body();
        var ragCompleteDto = gson.fromJson(responseBody, RAGCompleteDto.class);
        if (ragCompleteDto.getStatus() != 200) throw new HttpStatusException(
                "Webservice replied with an internally wrong status code, something went wrong there: " + ragCompleteDto.getStatus(), statusCode, url);

        return ragCompleteDto.getMessage();
    }

    /**
     * Gets the closest document embeddings from a given text controlled by the range variable
     *
     * @param range
     * @return
     */
    public List<DocumentEmbedding> getClosest3dDocumentEmbeddingsOfCorpus(float[] tsne3d, int range, long corpusId) throws SQLException {
        var query = "SELECT * FROM documentembeddings e "
                + "JOIN document d ON e.document_id = d.id "
                + "WHERE d.corpusid = ? "
                + "ORDER BY e.tsne3d <-> ? "
                + "LIMIT ?";
        var statement = vectorDbConnection.prepareStatement(query);
        statement.setObject(1, corpusId);
        statement.setObject(2, new PGvector(tsne3d));
        statement.setInt(3, range);
        var resultSet = statement.executeQuery();
        return buildDocumentEmbeddingsFromResultSet(resultSet);
    }

    /**
     * Gets the one embedding of a document. Can return NULL.
     *
     * @return
     */
    public DocumentEmbedding getDocumentEmbeddingOfDocument(long documentId) throws SQLException {
        var query = "SELECT * FROM documentembeddings WHERE document_id = ?";
        var statement = vectorDbConnection.prepareStatement(query);
        statement.setLong(1, documentId);
        var resultSet = statement.executeQuery();
        // We return the first found docucment embedding as there should be only one.
        var embeddings = buildDocumentEmbeddingsFromResultSet(resultSet);
        if (!embeddings.isEmpty()) return embeddings.stream().findFirst().get();
        return null;
    }

    /**
     * Given a list of docment, returns the list of DocumentEmbeddings from that.
     *
     * @param documentIds
     * @return
     */
    public List<DocumentEmbedding> getManyDocumentEmbeddingsOfDocuments(List<Long> documentIds) throws SQLException {
        List<DocumentEmbedding> embeddings = new ArrayList<>();
        if (documentIds == null || documentIds.isEmpty()) {
            return embeddings;
        }

        // Construct the query with an IN clause using placeholders
        // SQL injection isn't really a threat case here, but still, use the "?" syntax to ensure proper injection
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM documentembeddings WHERE document_id IN (");
        String placeholders = String.join(",", documentIds.stream().map(id -> "?").toArray(String[]::new));
        queryBuilder.append(placeholders).append(")");

        var query = queryBuilder.toString();
        var statement = vectorDbConnection.prepareStatement(query);

        // Set the document IDs in the prepared statement
        for (int i = 0; i < documentIds.size(); i++) {
            statement.setLong(i + 1, documentIds.get(i));
        }

        var resultSet = statement.executeQuery();
        embeddings = buildDocumentEmbeddingsFromResultSet(resultSet);
        return embeddings;
    }

    private ArrayList<DocumentEmbedding> buildDocumentEmbeddingsFromResultSet(ResultSet resultSet) throws SQLException {
        var embeddings = new ArrayList<DocumentEmbedding>();
        while (resultSet.next()) {
            var embedding = new DocumentEmbedding();
            embedding.setEmbedding(resultSet.getObject("embedding") != null ? ((PGvector) resultSet.getObject("embedding")).toArray() : null);
            embedding.setTsne3d(resultSet.getObject("tsne3d") != null ? ((PGvector) resultSet.getObject("tsne3d")).toArray() : null);
            embedding.setTsne2d(resultSet.getObject("tsne2d") != null ? ((PGvector) resultSet.getObject("tsne2d")).toArray() : null);
            embedding.setDocument_id(resultSet.getLong("document_id"));
            embedding.setId(resultSet.getLong("id"));

            if(embedding.getEmbedding() != null) embeddings.add(embedding);
        }
        return embeddings;
    }

    /**
     * Gets all embedding chunks of a document
     *
     * @return
     */
    public ArrayList<DocumentChunkEmbedding> getDocumentChunkEmbeddingsOfDocument(long documentId) throws SQLException {
        var query = "SELECT * FROM documentchunkembeddings WHERE document_id = ?";
        var statement = vectorDbConnection.prepareStatement(query);
        statement.setLong(1, documentId);
        var resultSet = statement.executeQuery();
        return buildDocumentChunkEmbeddingsFromResultSet(resultSet);
    }

    /**
     * Gets the closest document embeddings from a given text controlled by the range variable
     *
     * @param text
     * @param range
     * @return
     */
    public List<DocumentChunkEmbedding> getClosestDocumentChunkEmbeddings(String text, int range, long corpusId)
            throws SQLException, IOException, URISyntaxException, InterruptedException {
        // If the corpusid = -1, then we look at ANY document. Otherwise, only at those documentchunkembeddings from
        // a document that is in the corpus.
        var query = "";
        if (corpusId == -1) {
            query = "SELECT * FROM documentchunkembeddings e "
                    + "WHERE TRIM(COALESCE(e.coveredtext, '')) <> '' "  // Checks if coveredtext is not null and not empty
                    + "ORDER BY e.embedding <-> ? "
                    + "LIMIT ?";
        } else {
            // Filter by corpusid
            query = "SELECT * FROM documentchunkembeddings e "
                    + "JOIN document d ON e.document_id = d.id "
                    + "WHERE d.corpusid = ? and TRIM(COALESCE(e.coveredtext, '')) <> '' "
                    + "ORDER BY e.embedding <-> ? "
                    + "LIMIT ?";
        }
        var statement = vectorDbConnection.prepareStatement(query);
        if (corpusId == -1) {
            statement.setObject(1, new PGvector(getEmbeddingForText(text)));
            statement.setInt(2, range);
        } else {
            statement.setLong(1, corpusId);
            statement.setObject(2, new PGvector(getEmbeddingForText(text)));
            statement.setInt(3, range);
        }
        var resultSet = statement.executeQuery();
        return buildDocumentChunkEmbeddingsFromResultSet(resultSet);
    }

    private ArrayList<DocumentChunkEmbedding> buildDocumentChunkEmbeddingsFromResultSet(ResultSet resultSet) throws SQLException {
        var embeddings = new ArrayList<DocumentChunkEmbedding>();
        while (resultSet.next()) {
            var embedding = new DocumentChunkEmbedding(resultSet.getInt("beginn"), resultSet.getInt("endd"));
            embedding.setCoveredText(resultSet.getString("coveredtext"));
            embedding.setEmbedding(resultSet.getObject("embedding") != null ? ((PGvector) resultSet.getObject("embedding")).toArray() : null);
            embedding.setTsne3D(resultSet.getObject("tsne3d") != null ? ((PGvector) resultSet.getObject("tsne3d")).toArray() : null);
            embedding.setTsne2D(resultSet.getObject("tsne2d") != null ? ((PGvector) resultSet.getObject("tsne2d")).toArray() : null);
            embedding.setDocument_id(resultSet.getLong("document_id"));
            embedding.setId(resultSet.getLong("id"));

            if(embedding.getEmbedding() != null) embeddings.add(embedding);
        }
        return embeddings;
    }

    /**
     *  Returns true if the given document by its id has documentchunkembeddings in the database.
     */
    public boolean documentHasDocumentEmbedding(long documentId) throws SQLException {
        String query = "SELECT COUNT(*) FROM documentembeddings WHERE document_id = ?";
        try (var statement = vectorDbConnection.prepareStatement(query)) {
            statement.setLong(1, documentId);
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count > 0;
                }
            }
        }
        return false;
    }

    /**
     * Saves a document embedding.
     */
    public void saveDocumentEmbedding(DocumentEmbedding documentEmbedding) throws SQLException {
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
    public void updateDocumentEmbedding(DocumentEmbedding documentEmbedding) throws SQLException {
        String query = "UPDATE documentembeddings SET embedding = ?, tsne2d = ?, tsne3d = ? WHERE document_id = ?";
        executeUpdate(query,
                new PGvector(documentEmbedding.getEmbedding()),
                new PGvector(documentEmbedding.getTsne2d()),
                new PGvector(documentEmbedding.getTsne3d()),
                documentEmbedding.getDocument_id());
    }

    /**
     *  Returns true if the given document by its id has documentchunkembeddings in the database.
     */
    public boolean documentHasDocumentChunkEmbeddings(long documentId) throws SQLException {
        String query = "SELECT COUNT(*) FROM documentchunkembeddings WHERE document_id = ?";
        try (var statement = vectorDbConnection.prepareStatement(query)) {
            statement.setLong(1, documentId);
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count > 0;
                }
            }
        }
        return false;
    }

    /**
     * Saves a document chunk embedding.
     */
    public void saveDocumentChunkEmbedding(DocumentChunkEmbedding documentChunkEmbedding) throws SQLException {
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
    public void updateDocumentChunkEmbedding(DocumentChunkEmbedding documentChunkEmbedding) throws SQLException {
        String query = "UPDATE documentchunkembeddings SET embedding = ?, coveredtext = ?, beginn = ?, endd = ?, tsne2d = ?, tsne3d = ? WHERE id = ?";
        executeUpdate(query,
                new PGvector(documentChunkEmbedding.getEmbedding()),
                documentChunkEmbedding.getCoveredText(),
                documentChunkEmbedding.getBegin(),
                documentChunkEmbedding.getEnd(),
                new PGvector(documentChunkEmbedding.getTsne2D()),
                new PGvector(documentChunkEmbedding.getTsne3D()),
                documentChunkEmbedding.getId());
    }

    /**
     * Executes an update on the RAG database
     *
     */
    private void executeUpdate(String query, Object... params) throws SQLException {
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
    }

    /**
     * Gets a single DocumentEmbedding for a whole document.
     *
     * @param document
     * @return
     */
    public DocumentEmbedding getCompleteEmbeddingFromDocument(Document document) throws IOException, URISyntaxException, InterruptedException {
        var documentEmbedding = new DocumentEmbedding();
        documentEmbedding.setDocument_id(document.getId());
        documentEmbedding.setEmbedding(getEmbeddingForText(document.getFullText()));
        return documentEmbedding;
    }

    /**
     * Gets the complete and embedded lists of DocumentChunkEmbeddings for a single document
     */
    public List<DocumentChunkEmbedding> getCompleteEmbeddingChunksFromDocument(Document document) throws IOException, URISyntaxException, InterruptedException {
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
    public EmbeddingReduceDto getEmbeddingDimensionReductions(List<float[]> embeddings) throws IOException, InterruptedException, URISyntaxException {
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
        if (statusCode != 200) throw new HttpStatusException("Request returned invalid status code: " + statusCode, statusCode, url);

        var responseBody = response.body();
        var reductionDto = gson.fromJson(responseBody, EmbeddingReduceDto.class);
        if (reductionDto.getStatus() != 200) throw new HttpStatusException(
                "Webservice replied with an internally wrong status code, something went wrong there: " + reductionDto.getStatus(), statusCode, url);

        return reductionDto;
    }

    /**
     * A function that fetches the vector embeddings of a given text through our python webserver
     */
    public float[] getEmbeddingForText(String text) throws IOException, InterruptedException, URISyntaxException {
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
        var request = HttpRequest
                .newBuilder()
                .uri(new URI(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .timeout(Duration.ofSeconds(2))
                .build();

        // Send request and get response
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        var statusCode = response.statusCode();
        if (statusCode != 200) throw new HttpStatusException("Request returned invalid status code: " + statusCode, statusCode, url);

        var responseBody = response.body();
        var ragEmbedDto = gson.fromJson(responseBody, RAGEmbedDto.class);
        if (ragEmbedDto.getStatus() != 200) throw new HttpStatusException(
                "Webservice replied with an internally wrong status code, something went wrong there: " + ragEmbedDto.getStatus(), statusCode, url);

        return ragEmbedDto.getMessage();
    }

    /**
     * Gets a list of empty DocumentChunkEmbeddings with proper text splitting
     *
     * @param chunkSize Good size would be 1.200 for example
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
     * We are using postgresql vector extension, which won't work with hibernate. Hence, we open a different connection
     * solely for the RAG service.
     */
    private Connection setupVectorDbConnection() throws ClassNotFoundException, SQLException {
        Class.forName(config.getPostgresqlProperty("connection.driver_class"));
        var connection = DriverManager.getConnection(config.getPostgresqlProperty("hibernate.connection.url"),
                config.getPostgresqlProperty("hibernate.connection.username"),
                config.getPostgresqlProperty("hibernate.connection.password"));

        // After we have the connection, we set up some vector extension requirements.
        var setupStmt = connection.createStatement();
        setupStmt.executeUpdate("CREATE EXTENSION IF NOT EXISTS vector");
        PGvector.addVectorType(connection);

        return connection;
    }

}
