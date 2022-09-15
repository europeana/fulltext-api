package eu.europeana.fulltext.indexing.solr;

import static eu.europeana.fulltext.indexing.IndexingConstants.SOLR_QUERY_DEFAULT;

import eu.europeana.fulltext.exception.SolrServiceException;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.util.CollectionUtils;

public class SolrUtils {

  private static final Logger log = LogManager.getLogger(FulltextSolrService.class);

  /**
   * Gets the most recent  value for the specified timestampField, from all documents in the Fulltext collection
   *
   * @return Optional with the last update time
   * @throws SolrServiceException on Solr error
   */
  static Optional<Instant> getMostRecentValue(SolrClient solrClient, String timestampField) throws SolrServiceException {

    SolrQuery lastUpdateTimeQuery =
        new SolrQuery(SOLR_QUERY_DEFAULT)
            .addField(timestampField)
            .setRows(1)
            .setSort(timestampField, SolrQuery.ORDER.desc);


    QueryResponse response;
    try {
      response = solrClient.query(lastUpdateTimeQuery);
      if (log.isDebugEnabled()) {
        log.debug(
            "Performed Fulltext Solr search query in {}ms:  query={}",
            response.getElapsedTime(),
            lastUpdateTimeQuery);
      }
    } catch (IOException | SolrServerException ex) {
      throw new SolrServiceException(
          String.format(
              "Error while searching Fulltext Solr for lastUpdateTime. query=%s",
              lastUpdateTimeQuery.toString()),
          ex);
    }

    if (response == null || CollectionUtils.isEmpty(response.getResults())) {
      return Optional.empty();
    }

    Date fieldValue =
        (Date)
            response.getResults().get(0).getFieldValue(timestampField);

    return Optional.of(fieldValue.toInstant());
  }
}
