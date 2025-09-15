package org.texttechnologylab.uce.search;

import org.springframework.context.ApplicationContext;
import org.texttechnologylab.uce.common.models.authentication.UceUser;

import java.io.IOException;
import java.net.URISyntaxException;

public interface Search {
    public void fromSearchState(ApplicationContext serviceContext, String languageCode ,SearchState searchState) throws URISyntaxException, IOException;
    public void setSearchState(SearchState searchState);
    public SearchState initSearch(UceUser user);
    public SearchState getSearchHitsForPage(int page, UceUser user);
}
