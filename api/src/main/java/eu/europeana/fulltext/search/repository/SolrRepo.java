package eu.europeana.fulltext.search.repository;

import eu.europeana.fulltext.search.model.query.SolrDocument;
import org.springframework.data.solr.repository.SolrCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Boot repository for querying Solr
 *
 * @author Patrick Ehlert
 * Created on 28 May 2020
 */
@Repository
public interface SolrRepo extends SolrCrudRepository<SolrDocument, String>, SolrHighlightQuery {

   // no methods defined, we use the ones in SolrHighlightQueryImpl

}
