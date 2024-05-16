package org.texttechnologylab;

import com.mongodb.client.model.search.SearchHighlight;
import org.springframework.context.ApplicationContext;
import org.texttechnologylab.models.search.DocumentSearchResult;
import org.texttechnologylab.models.search.SearchLayer;
import org.texttechnologylab.models.search.SearchType;
import org.texttechnologylab.services.PostgresqlDataInterface_Impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * The search logic object for Semantic Role Search.
 * TODO: This class is pretty redundant to the Search_DefaultImpl class. The code should definitely be merged better!
 */
public class Search_SemanticRoleImpl implements Search{

    private PostgresqlDataInterface_Impl db;
    private SemanticRoleSearchState searchState;

    public Search_SemanticRoleImpl(ApplicationContext serviceContext, long corpusId,
                                   ArrayList<String> arg0,
                                   ArrayList<String> arg1,
                                   ArrayList<String> argm,
                                   String verb){
        this.db = serviceContext.getBean(PostgresqlDataInterface_Impl.class);

        this.searchState = new SemanticRoleSearchState(SearchType.SEMANTICROLE);
        this.searchState.setCorpusId(corpusId);
        this.searchState.setArg0(arg0);
        this.searchState.setArg1(arg1);
        this.searchState.setArgm(argm);
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
}
