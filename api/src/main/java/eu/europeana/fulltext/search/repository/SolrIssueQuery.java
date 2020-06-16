package eu.europeana.fulltext.search.repository;

import eu.europeana.fulltext.search.model.query.EuropeanaId;
import eu.europeana.fulltext.search.model.query.SolrNewspaper;
import org.springframework.data.solr.core.query.result.HighlightPage;

/**
 * Defines the query sent to solr to retrieve highlights in a particular newspaper issue (record)
 *
 * @author Patrick Ehlert
 * Created 29 May 2020
 */
public interface SolrIssueQuery {

    public HighlightPage<SolrNewspaper> findByEuropeanaIdAndQuery(EuropeanaId europeanaId, String query, int maxSnippets) ;

}
