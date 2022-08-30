package eu.europeana.fulltext.indexing.solr;

import static eu.europeana.fulltext.indexing.IndexingConstants.EUROPEANA_ID;
import static eu.europeana.fulltext.indexing.IndexingConstants.FULLTEXT_SOLR_BEAN;
import static eu.europeana.fulltext.indexing.IndexingConstants.SOLR_QUERY_DEFAULT;

import eu.europeana.fulltext.exception.SolrServiceException;
import eu.europeana.fulltext.indexing.IndexingConstants;
import eu.europeana.fulltext.indexing.config.IndexingAppSettings;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.response.schema.SchemaRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class FulltextSolrService implements InitializingBean {
  private static final Logger log = LogManager.getLogger(FulltextSolrService.class);

  private final SolrClient fulltextSolr;

  private final SolrQuery lastUpdateTimeQuery =
      new SolrQuery(SOLR_QUERY_DEFAULT)
          .addField(IndexingConstants.TIMESTAMP_UPDATE_METADATA)
          .setRows(1)
          .setSort(IndexingConstants.TIMESTAMP_UPDATE_METADATA, SolrQuery.ORDER.desc);

  private SchemaRepresentation schema;

  private final int metadataSolrSyncPageSize;

  public FulltextSolrService(
      @Qualifier(FULLTEXT_SOLR_BEAN) SolrClient fulltextSolr, IndexingAppSettings settings) {
    this.fulltextSolr = fulltextSolr;
    this.metadataSolrSyncPageSize = settings.getMetadataSolrSyncPageSize();
  }

  /**
   * Gets the most recent "timestamp_update" value from all documents in the Fulltext collection
   *
   * @return Optional with the last update time
   * @throws SolrServiceException on Solr error
   */
  public Optional<Instant> getLastUpdateTime() throws SolrServiceException {
    QueryResponse response;
    try {
      response = fulltextSolr.query(lastUpdateTimeQuery);
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
            response.getResults().get(0).getFieldValue(IndexingConstants.TIMESTAMP_UPDATE_METADATA);

    return Optional.of(fieldValue.toInstant());
  }

  /**
   * Checks if a document matching the given europeanaId exists in the collection
   *
   * @param europeanaId
   * @return
   * @throws SolrServerException
   * @throws IOException
   */
  public boolean existsByEuropeanaId(String europeanaId) throws SolrServiceException {
    SolrQuery query =
        new SolrQuery(EUROPEANA_ID + ":\"" + europeanaId + "\"").addField(EUROPEANA_ID);
    try {
      QueryResponse response = fulltextSolr.query(query);

      return response != null && !CollectionUtils.isEmpty(response.getResults());
    } catch (IOException | SolrServerException ex) {
      throw new SolrServiceException(
          String.format(
              "Error while checking for existing europeanaId. query=%s", query.toString()),
          ex);
    }
  }

  public void writeToSolr(List<SolrInputDocument> documents) throws SolrServiceException {

    try {
      UpdateResponse response = fulltextSolr.add(documents);
      if (log.isDebugEnabled()) {
        log.debug(
            "Wrote {} docs to Fulltext Solr in {}ms", documents.size(), response.getElapsedTime());
      }
    } catch (SolrServerException | IOException e) {
      throw new SolrServiceException("Exception during Solr insertion", e);
    }
  }

  public void deleteFromSolr(List<String> europeanaIds) throws SolrServiceException {
    try {
      UpdateResponse response = fulltextSolr.deleteById(europeanaIds);
      if (log.isDebugEnabled()) {
        log.debug(
            "Wrote {} docs to Fulltext Solr in {}ms",
            europeanaIds.size(),
            response.getElapsedTime());
      }
    } catch (SolrServerException | IOException e) {
      throw new SolrServiceException("Exception during Solr deletion", e);
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    SchemaRequest request = new SchemaRequest();
    SchemaResponse response = request.process(fulltextSolr);
    schema = response.getSchemaRepresentation();
  }

  public SchemaRepresentation getSchema() {
    return schema;
  }

  /**
   * Creates a Solr iterator for getting all documents in the Fulltext Solr collection.
   *
   * <p>Only populates the "europeana_id" field
   */
  public SolrSearchCursorIterator createFulltextSyncIterator() {
    return new SolrSearchCursorIterator(
        fulltextSolr,
        new SolrQuery("*:*")
            .setRows(metadataSolrSyncPageSize)
            .addField(EUROPEANA_ID)
            .setSort(EUROPEANA_ID, ORDER.asc));
  }
}
