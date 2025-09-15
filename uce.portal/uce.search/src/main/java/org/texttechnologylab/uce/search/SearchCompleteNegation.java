package org.texttechnologylab.uce.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.uce.common.config.CorpusConfig;
import org.texttechnologylab.uce.common.exceptions.ExceptionUtils;
import org.texttechnologylab.uce.common.models.authentication.UceUser;
import org.texttechnologylab.uce.common.models.dto.UCEMetadataFilterDto;
import org.texttechnologylab.uce.common.models.search.DocumentSearchResult;
import org.texttechnologylab.uce.common.models.search.SearchType;
import org.texttechnologylab.uce.common.services.PostgresqlDataInterface_Impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchCompleteNegation implements Search {
    private static final Logger logger = LogManager.getLogger(SearchCompleteNegation.class);
    private PostgresqlDataInterface_Impl db;
    private CompleteNegationSearchState searchState;

    public SearchCompleteNegation(ApplicationContext serviceContext,
                                  long corpusId,
                                  String searchQuery) throws ParseException {
        this.searchState = new CompleteNegationSearchState(SearchType.NEG);
        setDefaultSearchStateParameters(serviceContext, corpusId);

        // We need to parse the search query into the argument lists and save it in the state.
        parseSearchQueryAndFillSearchState(searchQuery);
    }
    public SearchCompleteNegation() {

    }

    public SearchCompleteNegation withUceMetadataFilters(List<UCEMetadataFilterDto> filters) {
        this.searchState.setUceMetadataFilters(filters);
        return this;
    }

    private void setDefaultSearchStateParameters(ApplicationContext serviceContext, long corpusId){
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.searchState.setCorpusId(corpusId);
        this.searchState.setCorpusConfig(ExceptionUtils.tryCatchLog(
                () -> CorpusConfig.fromJson(db.getCorpusById(corpusId).getCorpusJsonConfig()),
                (ex) -> logger.error("Error fetching the corpus and corpus config of corpus: " + corpusId, ex)));
    }

    /**
     * We need to parse the search queries of the negation search, which is it's own little query language.
     * The language is defined as follows:
     * - The query has to start with NEG::
     * - c=cue, s=scope, x=xscope, e=event, f=focus
     * - We have ARG0, ARG1, ARG2, ARGM and verb. We will use those like so: NEG::c=word1,word2;f=word1,word2;e=word1,s=word,x=word
     *
     * @param searchQuery
     * @return
     */
    private void parseSearchQueryAndFillSearchState(String searchQuery) throws ParseException {
        if (!searchQuery.startsWith("NEG::")) return;
        var workingCopy = searchQuery;

        // Delete the prelude
        workingCopy = workingCopy.replace("NEG::", "");
        var rawArgs = workingCopy.split(";"); // ["0=item1,item2", "1=item1,item2", ...]
        for (var i = 0; i < rawArgs.length; i++) {
            var curArg = rawArgs[i].trim();
            // We will use that later.
            var argType = curArg.charAt(0);

            // For now, parse the items
            var args = preprocess_args(new ArrayList<>(Arrays.stream(curArg.split("=")[1].split(",")).toList()));

            // Now store the args in the correct
            switch (argType) {
                case 'c':
                    this.searchState.setCue(args);
                    break;
                case 's':
                    this.searchState.setScope(args);
                    break;
                case 'x':
                    this.searchState.setXscope(args);
                    break;
                case 'e':
                    this.searchState.setEvent(args);
                    break;
                case 'f':
                    this.searchState.setFocus(args);
                    break;
                default:
                    throw new ParseException("Couldn't parse the semantic role query, argument types have to be 0, 1, 2 or m", 0);
            }
        }
    }

    /**
     * The args can be dirty with . or spaces or whatever
     *
     * @param argList
     */
    private ArrayList<String> preprocess_args(ArrayList<String> argList) {
        return new ArrayList<String>(argList.stream().map(a -> a
                        .trim()
                        .replace(".", "")
                        .toLowerCase())
                .toList());
    }

    /**
     * Executes a search request on the databases and returns a result object
     *
     * @param countAll determines whether we also count all search hits or just using pagination
     * @return
     */
    private DocumentSearchResult executeSearchOnDatabases(boolean countAll, UceUser user) {
        return ExceptionUtils.tryCatchLog(
                () -> db.completeNegationSearchForDocuments((searchState.getCurrentPage() - 1) * searchState.getTake(),
                        searchState.getTake(),
                        searchState.getCue(),
                        searchState.getEvent(),
                        searchState.getFocus(),
                        searchState.getScope(),
                        searchState.getXscope(),
                        countAll,
                        searchState.getOrder(),
                        searchState.getOrderBy(),
                        searchState.getCorpusId(),
                        searchState.getUceMetadataFilters(),
                        user),
                (ex) -> logger.error("Error executing semantic search on database.", ex));
    }

    @Override
    public SearchState initSearch(UceUser user) {
        var documentSearchResult = executeSearchOnDatabases(true, user);
        if (documentSearchResult == null)
            throw new NullPointerException("CompleteNegation Init Search returned null - not empty.");

        var documents = ExceptionUtils.tryCatchLog(
                () -> db.getManyDocumentsByIds(documentSearchResult.getDocumentIds()),
                (ex) -> logger.error("Error initializing the CompleteNegation search state - aborting creation and returning null.", ex));
        if (documents == null) return null;
        searchState.setCurrentDocuments(documents);
        searchState.setTotalHits(documentSearchResult.getDocumentCount());

        searchState.setFoundCues(documentSearchResult.getFoundCues());
        searchState.setFoundFoci(documentSearchResult.getFoundFoci());
        searchState.setFoundScopes(documentSearchResult.getFoundScopes());
        searchState.setFoundXScopes(documentSearchResult.getFoundXscopes());
        searchState.setFoundEvents(documentSearchResult.getFoundEvents());

        ArrayList<String> allSearchTokens = new ArrayList<>();
        allSearchTokens.addAll(searchState.getCue());
        allSearchTokens.addAll(searchState.getScope());
        allSearchTokens.addAll(searchState.getXscope());
        allSearchTokens.addAll(searchState.getEvent());
        allSearchTokens.addAll(searchState.getFocus());

        searchState.setSearchTokens(allSearchTokens);
        //searchState.setDocumentIdxToSnippets(documentSearchResult.getSearchSnippets());
        searchState.setDocumentIdToSnippets(documentSearchResult.getSearchSnippetsDocIdToSnippet());

        return searchState;
    }

    @Override
    public void fromSearchState(ApplicationContext serviceContext, String languageCode, SearchState searchState) throws URISyntaxException, IOException {
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.searchState = (CompleteNegationSearchState) searchState;
    }

    @Override
    public void setSearchState(SearchState searchState) {
        this.searchState = (CompleteNegationSearchState) searchState;
    }

    @Override
    public SearchState getSearchHitsForPage(int page, UceUser user) {
        // Adjust the current page and execute the search again
        this.searchState.setCurrentPage(page);
        var documentSearchResult = executeSearchOnDatabases(false, user);
        if (documentSearchResult == null)
            throw new NullPointerException("Neg Search returned NULL - not empty.");
        var documents = ExceptionUtils.tryCatchLog(
                () -> db.getManyDocumentsByIds(documentSearchResult.getDocumentIds()),
                (ex) -> logger.error("Error searching hits for page " + page + " in neg search - returning original search state.", ex));
        if (documents == null) return searchState;
        searchState.setCurrentDocuments(documents);

        searchState.setTotalHits(documentSearchResult.getDocumentCount());

        searchState.setFoundCues(documentSearchResult.getFoundCues());
        searchState.setFoundFoci(documentSearchResult.getFoundFoci());
        searchState.setFoundScopes(documentSearchResult.getFoundScopes());
        searchState.setFoundXScopes(documentSearchResult.getFoundXscopes());
        searchState.setFoundEvents(documentSearchResult.getFoundEvents());

        ArrayList<String> allSearchTokens = new ArrayList<>();
        allSearchTokens.addAll(searchState.getCue());
        allSearchTokens.addAll(searchState.getScope());
        allSearchTokens.addAll(searchState.getXscope());
        allSearchTokens.addAll(searchState.getEvent());
        allSearchTokens.addAll(searchState.getFocus());

        searchState.setSearchTokens(allSearchTokens);
        //searchState.setDocumentIdxToSnippets(documentSearchResult.getSearchSnippets());
        searchState.setDocumentIdToSnippets(documentSearchResult.getSearchSnippetsDocIdToSnippet());

        return searchState;
    }
}
