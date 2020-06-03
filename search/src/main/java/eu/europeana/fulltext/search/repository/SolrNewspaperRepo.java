package eu.europeana.fulltext.search.repository;

import eu.europeana.fulltext.search.model.query.EuropeanaId;
import eu.europeana.fulltext.search.model.query.SolrNewspaper;
import eu.europeana.fulltext.search.model.FTSearchDefinitions;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.data.solr.repository.Highlight;
import org.springframework.data.solr.repository.Query;
import org.springframework.data.solr.repository.SolrCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Boot repository for querying Solr
 *
 * @author Patrick Ehlert
 * Created on 28 May 2020
 */
@Repository
public interface SolrNewspaperRepo extends SolrCrudRepository<SolrNewspaper, String>, SolrEuropeanaId {

    // TODO find a way to set hl.maxAnalyzedChards and timeAllowed

    /**
     * Search the entire collection for fulltexts containing the word(s) specified in the query
     * @param query
     * @param page
     * @return HighlightPage containing documents with europeanaIds and hits
     */
    @Query(fields = {"europeana_id", "LANGUAGE" }) // no need to retrieve any other fields
    @Highlight(snipplets = 3, fields = {"europeana_id", "fulltext*"},
            prefix = FTSearchDefinitions.HIT_TAG_START, postfix = FTSearchDefinitions.HIT_TAG_END)
    HighlightPage<SolrNewspaper> findByFulltextIn(String query, Pageable page);

    /**
     * Retrieve one particular issue (CHO) and highlight its fulltext for one or more word(s) specified in the query
     * @param id
     * @param fullTextQuery
     * @param page
     */
    @Query(fields = {"europeana_id", "LANGUAGE" }) // no need to retrieve any other fields
    @Highlight(query = "?1", snipplets = 10, fields = {"europeana_id", "fulltext*"},
            prefix = FTSearchDefinitions.HIT_TAG_START, postfix = FTSearchDefinitions.HIT_TAG_END)
    HighlightPage<SolrNewspaper> findByEuropeanaId(EuropeanaId id, String fullTextQuery, Pageable page);

}
