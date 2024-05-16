package org.texttechnologylab;

import com.mongodb.client.model.search.SearchHighlight;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.texttechnologylab.models.search.DocumentSearchResult;
import org.texttechnologylab.models.search.SearchLayer;
import org.texttechnologylab.models.search.SearchType;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The search logic object for Semantic Role Search.
 * TODO: This class is pretty redundant to the Search_DefaultImpl class. The code should definitely be merged better!
 */
public class Search_SemanticRoleImpl implements Search{

    private PostgresqlDataInterface_Impl db;
    private SemanticRoleSearchState searchState;

    public Search_SemanticRoleImpl(ApplicationContext serviceContext, long corpusId,
                                   String searchQuery) throws ParseException {
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.searchState = new SemanticRoleSearchState(SearchType.SEMANTICROLE);
        this.searchState.setCorpusId(corpusId);

        // We need to parse the search query into the argument lists and save it in the state.
        parseSearchQueryAndFillSearchState(searchQuery);
    }

    public Search_SemanticRoleImpl(ApplicationContext serviceContext, long corpusId,
                                   ArrayList<String> arg0,
                                   ArrayList<String> arg1,
                                   ArrayList<String> argm,
                                   String verb){
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);

        this.searchState = new SemanticRoleSearchState(SearchType.SEMANTICROLE);
        this.searchState.setCorpusId(corpusId);
        this.searchState.setArg0(preprocess_args(arg0));
        this.searchState.setArg1(preprocess_args(arg1));
        this.searchState.setArgm(preprocess_args(argm));
        this.searchState.setVerb(verb);
    }

    public Search_SemanticRoleImpl(){}

    @Override
    public void fromSearchState(ApplicationContext serviceContext, SearchState searchState) throws URISyntaxException, IOException {
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);
        this.searchState = (SemanticRoleSearchState)searchState;
    }

    @Override
    public void setSearchState(SearchState searchState) {
        this.searchState = (SemanticRoleSearchState)searchState;
    }

    @Override
    public SearchState initSearch() {
        var documentSearchResult = executeSearchOnDatabases(true);
        if(documentSearchResult == null) throw new NullPointerException("Semantic Role Init Search returned null - not empty.");

        searchState.setCurrentDocuments(db.getManyDocumentsByIds(documentSearchResult.getDocumentIds()));
        searchState.setTotalHits(documentSearchResult.getDocumentCount());
        searchState.setFoundNamedEntities(documentSearchResult.getFoundNamedEntities());
        searchState.setFoundTaxons(documentSearchResult.getFoundTaxons());
        searchState.setFoundTimes(documentSearchResult.getFoundTimes());

        return searchState;
    }

    @Override
    public SearchState getSearchHitsForPage(int page) {
        // Adjust the current page and execute the search again
        this.searchState.setCurrentPage(page);
        var documentSearchResult = executeSearchOnDatabases(false);
        if(documentSearchResult == null) throw new NullPointerException("Semantic Role Search returned null - not empty.");
        searchState.setCurrentDocuments(db.getManyDocumentsByIds(documentSearchResult.getDocumentIds()));
        return searchState;
    }

    /**
     * We need to parse the search queries of the semantic role search, which is it's own little query language.
     * The language is defined as follows:
     * - The query has to start with SR::
     * - We have ARG0, ARG1, ARG2, ARGM and verb. We will those like so: SR::0=item1,item2; 1=item1,item2; m=item1,item2;v=asdasd
     * @param searchQuery
     * @return
     */
    private void parseSearchQueryAndFillSearchState(String searchQuery) throws ParseException {
        if(!searchQuery.startsWith("SR::")) return;
        var workingCopy = searchQuery;

        // Delete the prelude
        workingCopy = workingCopy.replace("SR::", "");
        var rawArgs = workingCopy.split(";"); // ["0=item1,item2", "1=item1,item2", ...]
        for(var i = 0; i < rawArgs.length; i++){
            var curArg = rawArgs[i].trim();
            // We will use that later.
            var argType = curArg.charAt(0);

            // For now, parse the items
            var args = preprocess_args(new ArrayList<>(Arrays.stream(curArg.split("=")[1].split(",")).toList()));

            // Now store the args in the correct
            switch (argType){
                case 'v':
                    this.searchState.setVerb(args.getFirst());
                    break;
                case '0':
                    this.searchState.setArg0(args);
                    break;
                case '1':
                    this.searchState.setArg1(args);
                    break;
                case '2':
                    this.searchState.setArg2(args);
                    break;
                case 'm':
                    this.searchState.setArgm(args);
                    break;
                default:
                    throw new ParseException("Couldn't parse the semantic role query, argument types have to be 0, 1, 2 or m", 0);
            }
        }
    }

    /**
     * Executes a search request on the databases and returns a result object
     * @param countAll determines whether we also count all search hits or just using pagination
     * @return
     */
    private DocumentSearchResult executeSearchOnDatabases(boolean countAll){
        return db.semanticRoleSearchForDocuments((searchState.getCurrentPage() - 1) * searchState.getTake(),
                searchState.getTake(),
                searchState.getArg0(),
                searchState.getArg1(),
                searchState.getArgm(),
                searchState.getVerb(),
                countAll,
                searchState.getOrder(),
                searchState.getOrderBy(),
                searchState.getCorpusId());
    }

    /**
     * The args can be dirty with . or spaces or whatever
     * @param argList
     */
    private ArrayList<String> preprocess_args(ArrayList<String> argList){
        return new ArrayList<String>(argList.stream().map(a -> a
                    .trim()
                    .replace(".", "")
                    .toLowerCase())
                .toList());
    }
}
