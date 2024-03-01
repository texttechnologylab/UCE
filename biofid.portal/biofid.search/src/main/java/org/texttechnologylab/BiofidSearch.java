package org.texttechnologylab;

import org.springframework.context.ApplicationContext;
import org.texttechnologylab.models.corpus.Document;
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
    private List<String> stopwords;
    private List<BiofidSearchLayer> searchLayers;
    private DatabaseService db = null;

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
     * Starts a new search with the Search instance
     * @param skip
     * @param take
     * @return
     */
    public List<Document> search(int skip, int take){
        var foundDocuments = new ArrayList<Document>();

        if(searchLayers.contains(BiofidSearchLayer.NAMED_ENTITIES)){
            foundDocuments.addAll(db.searchForDocuments(0, 15, searchTokens));
        }

        return foundDocuments;
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

