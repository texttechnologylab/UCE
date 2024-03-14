package org.texttechnologylab;

import org.springframework.context.ApplicationContext;
import org.texttechnologylab.models.search.DocumentSearchResult;
import org.texttechnologylab.models.search.OrderByColumn;
import org.texttechnologylab.models.search.SearchLayer;
import org.texttechnologylab.models.search.SearchOrder;
import org.texttechnologylab.services.DatabaseService;
import org.texttechnologylab.sparql.JenaSparqlFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Class that encapsulates all search layers within the biofid class
 */
public class BiofidSearch {
    private final BiofidSearchState biofidSearchState;
    private List<String> stopwords;
    private DatabaseService db;

    /**
     * Creates a new instance of the BiofidSearch, throws exceptions if components couldn't be inited.
     * @param searchPhrase
     * @throws URISyntaxException
     * @throws IOException
     */
    public BiofidSearch(ApplicationContext serviceContext,
                        String searchPhrase,
                        SearchLayer[] searchLayers) throws URISyntaxException, IOException {

        this.biofidSearchState = new BiofidSearchState();
        this.biofidSearchState.setSearchLayers(Arrays.stream(searchLayers).toList());
        // In case we want taxon search, we need to init the sparql database
        if(Arrays.stream(searchLayers).anyMatch(l -> l == SearchLayer.TAXON)){
            JenaSparqlFactory.initialize();
        }
        initServices(serviceContext);

        this.biofidSearchState.setSearchPhrase(searchPhrase);
        this.biofidSearchState.setSearchTokens(cleanSearchPhrase(searchPhrase));
    }

    public BiofidSearch(ApplicationContext serviceContext, BiofidSearchState biofidSearchState) throws URISyntaxException, IOException {
        initServices(serviceContext);
        this.biofidSearchState = biofidSearchState;
    }

    /**
     * Starts a new search with the Search instance and returns the first results of the search
     * @return
     */
    public BiofidSearchState initSearch(){
        DocumentSearchResult documentSearchResult = executeSearchOnDatabases(true);
        if(documentSearchResult == null) throw new NullPointerException("Document Init Search returned null - not empty.");
        biofidSearchState.setCurrentDocuments(db.getManyDocumentsByIds(documentSearchResult.getDocumentIds()));
        biofidSearchState.setTotalHits(documentSearchResult.getDocumentCount());
        biofidSearchState.setFoundNamedEntities(documentSearchResult.getFoundNamedEntities());
        biofidSearchState.setFoundTaxons(documentSearchResult.getFoundTaxons());
        biofidSearchState.setFoundTimes(documentSearchResult.getFoundTimes());
        return biofidSearchState;
    }

    /**
     * Returns the next X documents from the paginated search. Determine the page offset in the variable.
     * @return
     */
    public BiofidSearchState getSearchHitsForPage(int page){
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
        if(biofidSearchState.getSearchLayers().stream().count() == 1 && biofidSearchState.getSearchLayers().contains(SearchLayer.METADATA)){
            return db.searchForDocuments((biofidSearchState.getCurrentPage() - 1) * biofidSearchState.getTake(),
                    biofidSearchState.getTake(),
                    biofidSearchState.getSearchTokens(),
                    SearchLayer.METADATA,
                    countAll,
                    biofidSearchState.getOrder(),
                    biofidSearchState.getOrderBy());
        }

        // Execute the Named Entity search, which automatically executes metadata as well
        if(biofidSearchState.getSearchLayers().contains(SearchLayer.NAMED_ENTITIES)){
            return db.searchForDocuments((biofidSearchState.getCurrentPage() - 1) * biofidSearchState.getTake(),
                    biofidSearchState.getTake(),
                    biofidSearchState.getSearchTokens(),
                    SearchLayer.NAMED_ENTITIES,
                    countAll,
                    biofidSearchState.getOrder(),
                    biofidSearchState.getOrderBy());
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
        this.db = serviceContext.getBean(DatabaseService.class);
        // TODO: Add more language support in the future
        this.stopwords = loadStopwords("de-DE");
    }


}

