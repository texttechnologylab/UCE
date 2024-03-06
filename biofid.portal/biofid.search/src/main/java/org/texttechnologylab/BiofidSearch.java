package org.texttechnologylab;

import org.springframework.context.ApplicationContext;
import org.texttechnologylab.models.corpus.Document;
import org.texttechnologylab.models.search.DocumentSearchResult;
import org.texttechnologylab.services.DatabaseService;
import org.texttechnologylab.sparql.JenaSparqlFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Class that encapsulates all search layers within the biofid class
 */
public class BiofidSearch {
    private UUID searchId;
    /**
     * The raw search phrase
     */
    private String searchPhrase;
    private List<String> searchTokens;
    private List<BiofidSearchLayer> searchLayers;

    private Integer currentPage = 0;
    private final List<String> stopwords;
    private final DatabaseService db;
    private final Integer take = 15;

    /**
     * Creates a new instance of the BiofidSearch, throws exceptions if components couldn't be inited.
     * @param searchPhrase
     * @throws URISyntaxException
     * @throws IOException
     */
    public BiofidSearch(ApplicationContext serviceContext,
                        String searchPhrase,
                        BiofidSearchLayer[] searchLayers) throws URISyntaxException, IOException {

        this.searchLayers = Arrays.stream(searchLayers).toList();
        // In case we want taxon search, we need to init the sparql database
        if(Arrays.stream(searchLayers).anyMatch(l -> l == BiofidSearchLayer.TAXON)){
            JenaSparqlFactory.initialize();
        }

        this.searchId = UUID.randomUUID();
        this.db = serviceContext.getBean(DatabaseService.class);
        // TODO: Add more language support in the future
        this.stopwords = loadStopwords("de-DE");

        this.searchPhrase = searchPhrase;
        this.searchTokens = cleanSearchPhrase(searchPhrase);
    }

    /**
     * Starts a new search with the Search instance and returns the first results of the search
     * @return
     */
    public List<Document> initSearch(){
        DocumentSearchResult documentSearchResult = null;

        // Execute the metadata search. This layer is contained in the other layers, but there are some instances where
        // we ONLY want to use the metadata search, so handle that specific case here.
        if(searchLayers.stream().count() == 1 && searchLayers.contains(BiofidSearchLayer.METADATA)){
            documentSearchResult = db.searchForDocuments(currentPage, take, searchTokens, BiofidSearchLayer.METADATA.name().toLowerCase());
        }

        // Execute the Named Entity search, which automatically executes metadata as well
        if(searchLayers.contains(BiofidSearchLayer.NAMED_ENTITIES)){
            documentSearchResult = db.searchForDocuments(currentPage, take, searchTokens, BiofidSearchLayer.NAMED_ENTITIES.name().toLowerCase());
        }

        assert documentSearchResult != null;
        return db.getManyDocumentsByIds(documentSearchResult.getDocumentIds());
    }

    /**
     * Returns the next X documents from the paginated search. Determine the page offset in the variable.
     * @return
     */
    public List<Document> getSearchesForPage(int page){
        currentPage = page;

        // TODO: COntinue here: make the pagination happen

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

}

