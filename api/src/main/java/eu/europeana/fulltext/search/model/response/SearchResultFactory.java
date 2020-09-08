package eu.europeana.fulltext.search.model.response;

import eu.europeana.fulltext.search.model.response.v2.SearchResultV2;
import eu.europeana.fulltext.search.model.response.v3.SearchResultV3;

import static eu.europeana.fulltext.RequestUtils.REQUEST_VERSION_3;


public class SearchResultFactory {

    /**
     * Instantiates a SearchResult implementation based on the requestVersion provided.
     * Returns {@link SearchResultV2} by default
     *
     * @param searchId       id of SearchResult
     * @param debug          indicates whether SearchResult implementation should include debug info
     * @param requestVersion version of API request
     * @return SearchResult instance
     */
    public static SearchResult createSearchResult(String searchId, boolean debug, String requestVersion) {
        if (REQUEST_VERSION_3.equals(requestVersion)) {
            return new SearchResultV3(searchId, debug);
        }
        return new SearchResultV2(searchId, debug);
    }
}
