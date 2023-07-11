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
  private static final Logger LOGGER = LogManager.getLogger(FulltextSolrService.class);

  private final SolrClient fulltextSolr;
  private final int commitWithinMs;

  private SchemaRepresentation schema;

  private final int metadataSolrSyncPageSize;

  public FulltextSolrService(
      @Qualifier(FULLTEXT_SOLR_BEAN) SolrClient fulltextSolr, IndexingAppSettings settings) {
    this.fulltextSolr = fulltextSolr;
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
    SolrQuery query =
            new SolrQuery(EUROPEANA_ID + ":\"" + europeanaId + "\"").addField(EUROPEANA_ID);
    try {
      QueryResponse response = fulltextSolr.query(query);
      return response != null && !CollectionUtils.isEmpty(response.getResults());
    } catch (IOException | SolrServerException ex) {
      throw new SolrServiceException(
              String.format(
                      "Error while checking for existing europeanaId. query=%s", query.toString()), ex);
      }
  }

  public void writeToSolr(List<SolrInputDocument> documents) throws SolrServiceException {
    try {
      UpdateResponse response = fulltextSolr.add(documents, commitWithinMs);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Wrote {} docs to Fulltext Solr in {}ms; commitWithinMs={}",
                documents.size(), response.getElapsedTime(), commitWithinMs);
        }
      } catch (SolrServerException | IOException e) {
          throw new SolrServiceException("Exception during Solr insertion", e);
    }
  }

  public void deleteFromSolr(List<String> europeanaIds) throws SolrServiceException {
    if (europeanaIds == null || europeanaIds.isEmpty()) {
      return;
    }
    try {
      UpdateResponse response = fulltextSolr.deleteById(europeanaIds, commitWithinMs);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Deleted {} docs from Fulltext Solr in {}ms; commitWithinMs={}",
                europeanaIds.size(),
                response.getElapsedTime(), commitWithinMs);
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
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Performed Fulltext query in {}ms:  query={}", response.getElapsedTime(), query);
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
