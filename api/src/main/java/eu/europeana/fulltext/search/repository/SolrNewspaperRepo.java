package eu.europeana.fulltext.search.repository;

import eu.europeana.fulltext.search.model.query.SolrNewspaper;
import org.springframework.data.solr.repository.SolrCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Boot repository for querying Solr
 *
 * @author Patrick Ehlert
 * Created on 28 May 2020
 */
@Repository
public interface SolrNewspaperRepo extends SolrCrudRepository<SolrNewspaper, String>, SolrIssueQuery {

   // no methods defined, we use the ones in SolrEuropeanaQuery(Impl)
}
