package org.texttechnologylab;

import org.springframework.context.ApplicationContext;
import org.texttechnologylab.models.search.*;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.services.RAGService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class that encapsulates all search layers within the biofid class
 */
public class Search_DefaultImpl implements Search {
    private SearchState biofidSearchState;
    private List<String> stopwords;
    private PostgresqlDataInterface_Impl db;
    private RAGService ragService;

    /**
     * Creates a new instance of the BiofidSearch, throws exceptions if components couldn't be inited.
     * @param searchPhrase
     * @throws URISyntaxException
     * @throws IOException
     */
    public Search_DefaultImpl(ApplicationContext serviceContext,
                              String searchPhrase,
                              long corpusId,
                              List<SearchLayer> searchLayers) throws URISyntaxException, IOException {

        this.biofidSearchState = new SearchState(SearchType.DEFAULT);
        this.biofidSearchState.setSearchLayers(searchLayers);
        initServices(serviceContext);

        this.biofidSearchState.setCorpusId(corpusId);
        this.biofidSearchState.setSearchPhrase(searchPhrase);
        this.biofidSearchState.setSearchTokens(cleanSearchPhrase(searchPhrase));
    }

    public Search_DefaultImpl(){}

    public void fromSearchState(ApplicationContext serviceContext, SearchState biofidSearchState) throws URISyntaxException, IOException {
        initServices(serviceContext);
        setSearchState(biofidSearchState);
    }

    public void setSearchState(SearchState searchState){
        this.biofidSearchState = searchState;
    }

    /**
     * Starts a new search with the Search instance and returns the first results of the search
     * @return
     */
    public SearchState initSearch(){
        DocumentSearchResult documentSearchResult = executeSearchOnDatabases(true);
        if(documentSearchResult == null) throw new NullPointerException("Document Init Search returned null - not empty.");
        biofidSearchState.setCurrentDocuments(db.getManyDocumentsByIds(documentSearchResult.getDocumentIds()));
        biofidSearchState.setTotalHits(documentSearchResult.getDocumentCount());
        biofidSearchState.setFoundNamedEntities(documentSearchResult.getFoundNamedEntities());
        biofidSearchState.setFoundTaxons(documentSearchResult.getFoundTaxons());
        biofidSearchState.setFoundTimes(documentSearchResult.getFoundTimes());

        // Execute embedding search if desired.
        // This search is lose coupled from the rest and only done once in the initiation.
        if(biofidSearchState.getSearchLayers().contains(SearchLayer.EMBEDDINGS)){
            var closestDocumentsEmbeddings = ragService.getClosestDocumentChunkEmbeddings(
                    this.biofidSearchState.getSearchPhrase(),
                    20);

            var foundDocumentChunkEmbeddings = new ArrayList<DocumentChunkEmbeddingSearchResult>();
            for(var embedding:closestDocumentsEmbeddings){
                var document = db.getDocumentById(embedding.getDocument_id());
                var documentChunkEmbedding = new DocumentChunkEmbeddingSearchResult();
                documentChunkEmbedding.setDocument(document);
                documentChunkEmbedding.setDocumentChunkEmbedding(embedding);

                foundDocumentChunkEmbeddings.add(documentChunkEmbedding);
            }
            biofidSearchState.setFoundDocumentChunkEmbeddings(foundDocumentChunkEmbeddings);
        }

        return biofidSearchState;
    }

    /**
     * Returns the next X documents from the paginated search. Determine the page offset in the variable.
     * @return
     */
    public SearchState getSearchHitsForPage(int page){
        // Adjust the current page and execute the search again
        this.biofidSearchState.setCurrentPage(page);
        var documentSearchResult = executeSearchOnDatabases(false);
        if(documentSearchResult == null) throw new NullPointerException("Document Search returned null - not empty.");
        biofidSearchState.setCurrentDocuments(db.getManyDocumentsByIds(documentSearchResult.getDocumentIds()));
        return biofidSearchState;
    }

    /**
     * Executes a search request on the databases and returns a result object
     * @param countAll determines whether we also count all search hits or just using pagination
     * @return
     */
    private DocumentSearchResult executeSearchOnDatabases(boolean countAll){

        // Execute the metadata search. This layer is contained in the other layers, but there are some instances where
        // we ONLY want to use the metadata search, so handle that specific case here.
        if(!biofidSearchState.getSearchLayers().contains(SearchLayer.NAMED_ENTITIES) && biofidSearchState.getSearchLayers().contains(SearchLayer.METADATA)){
            return db.defaultSearchForDocuments((biofidSearchState.getCurrentPage() - 1) * biofidSearchState.getTake(),
                    biofidSearchState.getTake(),
                    biofidSearchState.getSearchTokens(),
                    SearchLayer.METADATA,
                    countAll,
                    biofidSearchState.getOrder(),
                    biofidSearchState.getOrderBy(),
                    biofidSearchState.getCorpusId());
        }

        // Execute the Named Entity search, which automatically executes metadata as well
        if(biofidSearchState.getSearchLayers().contains(SearchLayer.NAMED_ENTITIES)){
            return db.defaultSearchForDocuments((biofidSearchState.getCurrentPage() - 1) * biofidSearchState.getTake(),
                    biofidSearchState.getTake(),
                    biofidSearchState.getSearchTokens(),
                    SearchLayer.NAMED_ENTITIES,
                    countAll,
                    biofidSearchState.getOrder(),
                    biofidSearchState.getOrderBy(),
                    biofidSearchState.getCorpusId());
        }

        return null;
    }

    /**
     * Loads the appropriate stopwords from the resources
     *
     * @param languageCode de-DE for german
     */
    private List<String> loadStopwords(String languageCode) throws URISyntaxException, IOException {
        return Files.readAllLines(Paths.get(getClass().getClassLoader().getResource("stopwords_" + languageCode + ".txt").toURI()));
    }

    /**
     * Cleans the search phrase like stopwords removal, trimming
     *
     * @return
     */
    private List<String> cleanSearchPhrase(String search) {

        search = search.trim().toLowerCase();
        var splited = Arrays.stream(search.split(" ")).toList();
        // Remove all stopwords
        splited = splited.stream().filter(s -> !stopwords.contains(s)).toList();

        return splited;
    }

    private void initServices(ApplicationContext serviceContext) throws URISyntaxException, IOException {
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.ragService = serviceContext.getBean(RAGService.class);
        // TODO: Add more language support in the future
        this.stopwords = loadStopwords("de-DE");
    }


}

