package org.texttechnologylab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.config.CorpusConfig;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.corpus.UCEMetadataFilter;
import org.texttechnologylab.models.dto.UceMetadataFilterDto;
import org.texttechnologylab.models.search.*;
import org.texttechnologylab.services.JenaSparqlService;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;
import org.texttechnologylab.services.RAGService;
import org.texttechnologylab.utils.Stopwords;
import org.texttechnologylab.utils.StringUtils;
import org.texttechnologylab.utils.SystemStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class that encapsulates all search layers within the biofid class
 */
public class Search_DefaultImpl implements Search {
    private static final Logger logger = LogManager.getLogger();
    private SearchState searchState;
    private List<String> stopwords;
    private PostgresqlDataInterface_Impl db;
    private RAGService ragService;
    private JenaSparqlService jenaSparqlService;

    /**
     * Creates a new instance of the Search_DefaultImpl, throws exceptions if components couldn't be inited.
     *
     * @param searchPhrase
     * @throws URISyntaxException
     * @throws IOException
     */
    public Search_DefaultImpl(ApplicationContext serviceContext,
                              String searchPhrase,
                              long corpusId,
                              String languageCode,
                              List<SearchLayer> searchLayers,
                              boolean enrichSearchTerm) throws URISyntaxException, IOException {
        this.searchState = new SearchState(SearchType.DEFAULT);
        this.searchState.setSearchLayers(searchLayers);
        initServices(serviceContext, languageCode);

        this.searchState.setCorpusId(corpusId);
        this.searchState.setCorpusConfig(ExceptionUtils.tryCatchLog(
                () -> CorpusConfig.fromJson(db.getCorpusById(corpusId).getCorpusJsonConfig()),
                (ex) -> logger.error("Error fetching the corpus and corpus config of corpus: " + corpusId, ex)));
        this.searchState.setSearchPhrase(searchPhrase);

        var cleanedSearchPhrase = cleanSearchPhrase(searchPhrase);
        if(enrichSearchTerm) this.searchState.setSearchTokens(enrichSearchTokens(removeStopwords(cleanedSearchPhrase)));
        else {
            var searchTokens = new ArrayList<String>();
            searchTokens.add(String.join(" ", cleanedSearchPhrase));
            this.searchState.setSearchTokens(searchTokens);
        }
    }

    public Search_DefaultImpl() {
    }

    public Search_DefaultImpl withUceMetadataFilters(List<UceMetadataFilterDto> filters){
        this.searchState.setUceMetadataFilters(filters);
        return this;
    }

    public void fromSearchState(ApplicationContext serviceContext,
                                String languageCode,
                                SearchState searchState) throws URISyntaxException, IOException {
        initServices(serviceContext, languageCode);
        setSearchState(searchState);
    }

    public SearchState getSearchState() {
        return this.searchState;
    }

    public void setSearchState(SearchState searchState) {
        this.searchState = searchState;
    }

    /**
     * Starts a new search with the Search instance and returns the first results of the search
     *
     * @return
     */
    public SearchState initSearch() {
        DocumentSearchResult documentSearchResult = executeSearchOnDatabases(true);
        if (documentSearchResult == null)
            throw new NullPointerException("Document Init Search returned null - not empty.");

        var documents = ExceptionUtils.tryCatchLog(() -> db.getManyDocumentsByIds(documentSearchResult.getDocumentIds()),
                (ex) -> logger.error("Error getting many documents by a list of ids in the search init. " +
                        "Search can't be created hence.", ex));
        if (documents == null) return null;
        searchState.setCurrentDocuments(documents);
        searchState.setCurrentDocumentHits(documentSearchResult.getDocumentHits());
        searchState.setDocumentIdxToSnippet(documentSearchResult.getSearchSnippets());
        searchState.setDocumentIdxToRank(documentSearchResult.getSearchRanks());
        searchState.setTotalHits(documentSearchResult.getDocumentCount());
        searchState.setFoundNamedEntities(documentSearchResult.getFoundNamedEntities());
        searchState.setFoundTaxons(documentSearchResult.getFoundTaxons());
        searchState.setFoundTimes(documentSearchResult.getFoundTimes());

        // Execute embedding search if desired.
        // This search is lose coupled from the rest and only done once in the initiation.
        if (searchState.getSearchLayers().contains(SearchLayer.EMBEDDINGS)) {
            var closestDocumentsEmbeddings = ExceptionUtils.tryCatchLog(
                    () -> ragService.getClosestDocumentChunkEmbeddings(
                            this.searchState.getOriginalSearchQuery(),
                            20,
                            this.searchState.getCorpusId()),
                    (ex) -> logger.error("Error getting the closest document chunk embeddings of the searchphrase: " + this.searchState.getOriginalSearchQuery(), ex));

            if (closestDocumentsEmbeddings == null) return searchState;
            var foundDocumentChunkEmbeddings = new ArrayList<DocumentChunkEmbeddingSearchResult>();
            for (var embedding : closestDocumentsEmbeddings) {
                var document = ExceptionUtils.tryCatchLog(() -> db.getDocumentById(embedding.getDocument_id()),
                        (ex) -> logger.error("Error fetching a document by its id for the search init with embeddings.", ex));
                if (document == null) continue;
                var documentChunkEmbedding = new DocumentChunkEmbeddingSearchResult();
                documentChunkEmbedding.setDocument(document);
                documentChunkEmbedding.setDocumentChunkEmbedding(embedding);

                foundDocumentChunkEmbeddings.add(documentChunkEmbedding);
            }
            searchState.setFoundDocumentChunkEmbeddings(foundDocumentChunkEmbeddings);
        }

        return searchState;
    }

    /**
     * Returns the next X documents from the paginated search. Determine the page offset in the variable.
     *
     * @return
     */
    public SearchState getSearchHitsForPage(int page) {
        // Adjust the current page and execute the search again
        this.searchState.setCurrentPage(page);
        var documentSearchResult = executeSearchOnDatabases(false);
        if (documentSearchResult == null) throw new NullPointerException("Document Search returned null - not empty.");
        var documents = ExceptionUtils.tryCatchLog(() -> db.getManyDocumentsByIds(documentSearchResult.getDocumentIds()),
                (ex) -> logger.error("Error getting many documents by a list of ids while getting hits for page " + page +
                        " hence returning the last state.", ex));
        if (documents == null) return searchState;
        searchState.setCurrentDocuments(documents);
        searchState.setCurrentDocumentHits(documentSearchResult.getDocumentHits());
        searchState.setDocumentIdxToSnippet(documentSearchResult.getSearchSnippets());
        searchState.setDocumentIdxToRank(documentSearchResult.getSearchRanks());
        return searchState;
    }

    /**
     * Executes a search request on the databases and returns a result object
     *
     * @param countAll determines whether we also count all search hits or just using pagination
     * @return
     */
    private DocumentSearchResult executeSearchOnDatabases(boolean countAll) {
        if (searchState.getSearchLayers().contains(SearchLayer.FULLTEXT)) {
            return ExceptionUtils.tryCatchLog(
                    () -> db.defaultSearchForDocuments((searchState.getCurrentPage() - 1) * searchState.getTake(),
                            searchState.getTake(),
                            searchState.getOriginalSearchQuery(),
                            searchState.getSearchTokens(),
                            SearchLayer.FULLTEXT,
                            countAll,
                            searchState.getOrder(),
                            searchState.getOrderBy(),
                            searchState.getCorpusId(),
                            searchState.getUceMetadataFilters()),
                    (ex) -> logger.error("Error executing a search on the database with search layer METADATA. Search can't be executed.", ex));
        }

        // Execute the Named Entity search
        if (searchState.getSearchLayers().contains(SearchLayer.NAMED_ENTITIES)) {
            return ExceptionUtils.tryCatchLog(
                    () -> db.defaultSearchForDocuments((searchState.getCurrentPage() - 1) * searchState.getTake(),
                            searchState.getTake(),
                            searchState.getOriginalSearchQuery(),
                            searchState.getSearchTokens(),
                            SearchLayer.NAMED_ENTITIES,
                            countAll,
                            searchState.getOrder(),
                            searchState.getOrderBy(),
                            searchState.getCorpusId(),
                            searchState.getUceMetadataFilters()),
                    (ex) -> logger.error("Error executing a search on the database with search layer NAMED_ENTITIES. Search can't be executed.", ex));
        }

        return null;
    }

    /**
     * Loads the appropriate stopwords from the resources. Loads them once and then caches them in RAM
     *
     * @param languageCode de-DE for german
     */
    private List<String> loadStopwords(String languageCode) throws IOException {
        // See if we have them cached. Else, load them once.
        var cachedStopwords = Stopwords.GetStopwords(languageCode);
        if (cachedStopwords != null) return cachedStopwords;

        List<String> stopwords = new ArrayList<>();
        try (var inputStream = getClass().getClassLoader().getResourceAsStream("stopwords_" + languageCode + ".txt");
             var reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stopwords.add(line);
            }
        } catch (NullPointerException e) {
            throw new RuntimeException("stopwords_" + languageCode + ".txt not found in the classpath");
        }

        // Cache them
        logger.info("Cached the stopwords for language " + languageCode + " - will not fetch them anymore now.");
        Stopwords.SetStopwords(languageCode, stopwords);
        return Stopwords.GetStopwords(languageCode);
    }

    /**
     * Possibly enriches search tokens with taxon ontologies. The function may produce more tokens in the end.
     *
     * @param tokens
     * @return
     */
    private List<String> enrichSearchTokens(List<String> tokens) {
        var finalTokens = new ArrayList<>(tokens);

        // Check for potential ontology alternative names. This can only work if our jena sparql db is running
        // and we have taxonomy annotated.
        if (SystemStatus.JenaSparqlStatus.isAlive() && this.searchState.getCorpusConfig() != null && this.searchState.getCorpusConfig().getAnnotations().getTaxon().isBiofidOnthologyAnnotated()) {
            var potentialTaxons = ExceptionUtils.tryCatchLog(
                    () -> db.getIdentifiableTaxonsByValues(tokens.stream().map(String::toLowerCase).toList()),
                    (ex) -> logger.error("Error trying to fetch taxons based on a list of tokens.", ex));

            if (potentialTaxons == null || potentialTaxons.isEmpty()) return tokens;

            potentialTaxons.forEach(t -> {
                if(!finalTokens.contains(t.getCoveredText())) finalTokens.add(t.getCoveredText());
            });

            var ids = new ArrayList<String>();
            for (var taxon : potentialTaxons) {
                if (taxon.getIdentifier().contains("|") || taxon.getIdentifier().contains(" ")) {
                    ids.addAll(taxon.getIdentifierAsList());
                } else {
                    ids.add(taxon.getIdentifier().trim());
                }
            }
            var newTokens = ExceptionUtils.tryCatchLog(
                    () ->jenaSparqlService.getAlternativeNamesOfTaxons(ids),
                    (ex) -> logger.error("Error getting the alt names of a taxon while searching. Operation continues.", ex));
            if (newTokens != null) finalTokens.addAll(newTokens);
        }

        return finalTokens;
    }

    /**
     * Cleans the search phrase like stopwords removal, trimming
     *
     * @return
     */
    private List<String> cleanSearchPhrase(String search) {
        search = search.trim();
        var splited = Arrays.stream(search.split(" ")).toList();
        splited = splited.stream().map(StringUtils::removeSpecialCharactersAtEdges).toList();
        return splited;
    }

    private List<String> removeStopwords(List<String> searchTokens){
        return searchTokens.stream().filter(s -> !stopwords.contains(s.toLowerCase())).toList();
    }

    private void initServices(ApplicationContext serviceContext, String languageCode) throws URISyntaxException, IOException {
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.ragService = serviceContext.getBean(RAGService.class);
        this.jenaSparqlService = serviceContext.getBean(JenaSparqlService.class);
        this.stopwords = loadStopwords(languageCode);
    }

}

