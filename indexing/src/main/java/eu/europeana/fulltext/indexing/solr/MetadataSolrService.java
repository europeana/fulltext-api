package eu.europeana.fulltext.indexing.solr;

import static eu.europeana.fulltext.indexing.IndexingConstants.ALL;
import static eu.europeana.fulltext.indexing.IndexingConstants.METADATA_SOLR_BEAN;

import eu.europeana.fulltext.exception.SolrServiceException;
import eu.europeana.fulltext.indexing.IndexingConstants;
import eu.europeana.fulltext.indexing.config.IndexingAppSettings;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class MetadataSolrService {

  private static final Logger log = LogManager.getLogger(MetadataSolrService.class);

  private final SolrClient metadataSolr;

  private final int retryLimit;

  public MetadataSolrService(@Qualifier(METADATA_SOLR_BEAN) SolrClient metadataSolr, IndexingAppSettings settings) {
    this.metadataSolr = metadataSolr;
    this.retryLimit = settings.getBatchRetryLimit();
  }

  public SolrDocument getDocument(String europeanaId) throws SolrServiceException {
    int attempts = 1;

    SolrQuery query =
        new SolrQuery(IndexingConstants.EUROPEANA_ID + ":\"" + europeanaId + "\"").addField(ALL);

    while (attempts <= retryLimit) {
      try {
        QueryResponse response = metadataSolr.query(query);
        if (log.isDebugEnabled()) {
          log.debug("Performed Metadata query in {}ms:  query={}; attempt={}", response.getElapsedTime(),
              query, attempts);
        }
        if (response != null && !response.getResults().isEmpty()) {
          return response.getResults().get(0);
        }
        break;
      } catch (SolrServerException | IOException e) {
        attempts++;
        if (attempts > retryLimit) {
          throw new SolrServiceException(
              String.format(
                  "Error while searching Solr for lastUpdateTime after %s attempts. query=%s", attempts-1, query.toString()),
              e);
        }

        try {
          Thread.sleep(IndexingConstants.SLEEP_MS);
        } catch (InterruptedException e1) {
          Thread.currentThread().interrupt();
        }
      }
    }

    return new SolrDocument();
  }

  /**
   * Gets the most recent value for the specified timestampField, from all documents in the Metadata
   * collection
   *
   * @return Optional with the last update time
   * @throws SolrServiceException on Solr error
   */
  public Optional<Instant> getMostRecentValue(String timestampField) throws SolrServiceException {
    return SolrUtils.getMostRecentValue(metadataSolr, timestampField);
  }
}
