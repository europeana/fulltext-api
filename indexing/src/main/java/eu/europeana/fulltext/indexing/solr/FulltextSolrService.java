package eu.europeana.fulltext.indexing.solr;

import eu.europeana.fulltext.exception.SolrServiceException;
import eu.europeana.fulltext.indexing.IndexingConstants;
import eu.europeana.fulltext.indexing.config.IndexingAppSettings;
import java.io.IOException;
import java.time.Instant;
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
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import static eu.europeana.fulltext.indexing.IndexingConstants.*;

@Service
public class FulltextSolrService implements InitializingBean {
  private static final Logger log = LogManager.getLogger(FulltextSolrService.class);

  private final SolrClient fulltextSolr;
  private final int commitWithinMs;
  private final int retryLimit;

  private SchemaRepresentation schema;

  private final int metadataSolrSyncPageSize;

  public FulltextSolrService(
          @Qualifier(FULLTEXT_SOLR_BEAN) SolrClient fulltextSolr, IndexingAppSettings settings) {
    this.fulltextSolr = fulltextSolr;
    this.retryLimit = settings.getRetryLimit();
    this.metadataSolrSyncPageSize = settings.getMetadataSolrSyncPageSize();
    this.commitWithinMs = settings.getCommitWithinMs();
  }

  /**
   * Gets the most recent value for the specified timestampField, from all documents in the Fulltext
   * collection
   *
   * @return Optional with the last update time
   * @throws SolrServiceException on Solr error
   */
  public Optional<Instant> getMostRecentValue(String timestampField) throws SolrServiceException {
    return SolrUtils.getMostRecentValue(fulltextSolr, timestampField);
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
    int attempts = retryLimit;
    SolrQuery query =
            new SolrQuery(EUROPEANA_ID + ":\"" + europeanaId + "\"").addField(EUROPEANA_ID);
    while (attempts > 0) {
      try {
        QueryResponse response = fulltextSolr.query(query);
        return response != null && !CollectionUtils.isEmpty(response.getResults());
      } catch (IOException | SolrServerException ex) {
        attempts--;
        if (attempts <= 0) {
          throw new SolrServiceException(
                  String.format(
                          "Error while checking for existing europeanaId. query=%s", query.toString()),
                  ex);
        }
        try {
          Thread.sleep(IndexingConstants.SLEEP_MS);
        } catch (InterruptedException e1) {
          throw new SolrServiceException("Error while checking for existing europeanaId", e1);
        }

      }

    }
  return false;
  }

  public void writeToSolr(List<SolrInputDocument> documents) throws SolrServiceException {
    int attempts = retryLimit;
    while (attempts > 0) {
      try {
        UpdateResponse response = fulltextSolr.add(documents, commitWithinMs);
        if (log.isDebugEnabled()) {
          log.debug(
                  "Wrote {} docs to Fulltext Solr in {}ms; commitWithinMs={}", documents.size(), response.getElapsedTime(), commitWithinMs);
        }
        break;
      } catch (SolrServerException | IOException e) {
        attempts--;
        if (attempts <= 0) {
          throw new SolrServiceException("Exception during Solr insertion", e);
        }
        try {
          Thread.sleep(IndexingConstants.SLEEP_MS);
        } catch (InterruptedException e1) {
          throw new SolrServiceException("Exception during Solr insertion", e1);
        }
      }
    }
  }

  public void deleteFromSolr(List<String> europeanaIds) throws SolrServiceException {
    if (europeanaIds == null || europeanaIds.isEmpty()) {
      return;
    }
    int attempts = retryLimit;
    while (attempts > 0) {

      try {
        UpdateResponse response = fulltextSolr.deleteById(europeanaIds, commitWithinMs);
        if (log.isDebugEnabled()) {
          log.debug(
                  "Deleted {} docs from Fulltext Solr in {}ms; commitWithinMs={}",
                  europeanaIds.size(),
                  response.getElapsedTime(), commitWithinMs);
        }
        break;
      } catch (SolrServerException | IOException e) {
        attempts--;
        if (attempts <= 0) {
          throw new SolrServiceException("Exception during Solr deletion", e);
        }
        try {
          Thread.sleep(IndexingConstants.SLEEP_MS);
        } catch (InterruptedException e1) {
          throw new SolrServiceException("Exception during Solr deletion", e1);
        }
      }
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
   * @param fields fields to populate
   */
  public SolrSearchCursorIterator createFulltextSyncIterator(String... fields) {
    return new SolrSearchCursorIterator(
        fulltextSolr,
        new SolrQuery("*:*")
            .setRows(metadataSolrSyncPageSize)
            .setFields(fields)
            .setSort(EUROPEANA_ID, ORDER.asc));
  }

  public SolrDocument getDocument(String europeanaId) throws SolrServiceException {
    QueryResponse response;

    SolrQuery query =
            new SolrQuery(IndexingConstants.EUROPEANA_ID + ":\"" + europeanaId + "\"").addField(ALL);

    try {
      response = fulltextSolr.query(query);
      if (log.isDebugEnabled()) {
        log.debug("Performed Fulltext query in {}ms:  query={}", response.getElapsedTime(), query);
      }

      if (response != null && !response.getResults().isEmpty()) {
        return response.getResults().get(0);
      }

    } catch (IOException | SolrServerException ex) {
      throw new SolrServiceException(
              String.format(
                      "Error while searching Solr for fulltext document. query=%s", query.toString()),
              ex);
    }
    return new SolrDocument();
  }

  }
