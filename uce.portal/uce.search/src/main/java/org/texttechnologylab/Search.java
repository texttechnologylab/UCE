package org.texttechnologylab;

import org.springframework.context.ApplicationContext;
import org.texttechnologylab.models.search.SearchType;

import java.io.IOException;
import java.net.URISyntaxException;

public interface Search {
    public void fromSearchState(ApplicationContext serviceContext, String languageCode ,SearchState searchState) throws URISyntaxException, IOException;
    public void setSearchState(SearchState searchState);
    public SearchState initSearch();
    public SearchState getSearchHitsForPage(int page);
}
