package eu.europeana.fulltext.indexing.solr;

import static eu.europeana.fulltext.indexing.IndexingConstants.ALL;
import static eu.europeana.fulltext.indexing.IndexingConstants.METADATA_SOLR_BEAN;

import eu.europeana.fulltext.exception.SolrServiceException;
import eu.europeana.fulltext.indexing.IndexingConstants;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
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

  public MetadataSolrService(@Qualifier(METADATA_SOLR_BEAN) SolrClient metadataSolr) {
    this.metadataSolr = metadataSolr;
  }

  public SolrDocument getDocument(String europeanaId) throws SolrServiceException {
    QueryResponse response;

    SolrQuery query =
        new SolrQuery(IndexingConstants.EUROPEANA_ID + ":\"" + europeanaId + "\"").addField(ALL);

    try {
      response = metadataSolr.query(query);
      if (log.isDebugEnabled()) {
        log.debug("Performed Metadata query in {}ms:  query={}", response.getElapsedTime(), query);
      }

      if (response != null && !response.getResults().isEmpty()) {
        return response.getResults().get(0);
      }

    } catch (IOException | SolrServerException ex) {
      throw new SolrServiceException(
          String.format(
              "Error while searching Solr for lastUpdateTime. query=%s", query.toString()),
          ex);
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
