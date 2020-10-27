package eu.europeana.fulltext.search.repository;

import eu.europeana.fulltext.api.service.exception.FTException;
import eu.europeana.fulltext.search.model.query.EuropeanaId;
import eu.europeana.fulltext.search.model.response.Debug;

import java.util.List;
import java.util.Map;

/**
 * Defines the query sent to solr to retrieve highlights in a particular newspaper issue (record)
 *
 * @author Patrick Ehlert
 * Created 29 May 2020
 */
public interface SolrHighlightQuery {

    /**
     * Send a query to a Solr server/cluster that is modified to return highlight with offsets information
     * @param europeanaId    europeana id of the issue to search
     * @param query          the string to search
     * @param maxSnippets    maximum number of snippets we want from solr
     * @param debug          if not null we store debug information in the object
     * @return Map containing snippets and passages (offsets)
     * @throws FTException when there's an error sending/reading the request to/from Solr
     */
    Map<String, List<String>> getHighlightsWithOffsets(EuropeanaId europeanaId, String query, int maxSnippets,
                                                       Debug debug) throws FTException;

}
