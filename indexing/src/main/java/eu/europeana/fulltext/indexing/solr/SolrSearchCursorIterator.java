package eu.europeana.fulltext.indexing.solr;

import eu.europeana.fulltext.indexing.IndexingConstants;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Fetches documents from Solr using a cursor. See
 * https://solr.apache.org/guide/7_6/pagination-of-results.html#fetching-a-large-number-of-sorted-results-cursors
 */
public class SolrSearchCursorIterator implements Iterator<SolrDocumentList> {

  private static final Logger LOGGER = LogManager.getLogger(SolrSearchCursorIterator.class);

  private final SolrClient client;
  private final SolrQuery solrQuery;

  private String cursorMark;
  private String previousCursorMark;

  public SolrSearchCursorIterator(SolrClient client, SolrQuery solrQuery) {
    validateQueryFields(solrQuery);
    ensureSortClause(solrQuery);

    this.solrQuery = solrQuery;
    this.client = client;
    this.cursorMark = CursorMarkParams.CURSOR_MARK_START;
  }

  /**
   * Checks if additional documents can be retrieved for the Solr query
   *
   * @return true if documents can be retrieved, false otherwise
   */
  public boolean hasNext() {
    return !cursorMark.equals(previousCursorMark);
  }

  /** Retrieves the next chunk of documents that match the search query. */
  public SolrDocumentList next() {
    solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
    QueryResponse response = null;
    int attempts = IndexingConstants.ATTEMPTS;
    while (attempts > 0) {
      try {
        response = client.query(solrQuery);
        break;
      } catch (SolrServerException | IOException ex) {
        attempts--;
        if (attempts <= 0) {
          throw new IllegalStateException(
                  String.format("Error while searching Solr q=%s", solrQuery.getQuery()), ex);
        }
        try {
          Thread.sleep(IndexingConstants.SLEEP_MS);
        } catch (InterruptedException e1) {
          throw new IllegalStateException("Error while searching Solr", e1);
        }

      }
    }
    previousCursorMark = cursorMark;
    cursorMark = response.getNextCursorMark();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
              "Performed Solr search query in {}ms: numFound={}, cursorMark={}, q={}",
              response.getElapsedTime(),
              response.getResults().getNumFound(),
              cursorMark,
              solrQuery.getQuery());
    }
    return response.getResults();
  }


  public SolrQuery getQuery() {
    return solrQuery;
  }

  /**
   * If query specifies fields, "europeana_id" must be included
   *
   * @param solrQuery query object
   */
  private void validateQueryFields(SolrQuery solrQuery) {
    String fieldString = solrQuery.getFields();

    if (!StringUtils.hasLength(fieldString)) {
      return;
    }

    List<String> fields = Arrays.asList(fieldString.split(","));
    if (!fields.contains(IndexingConstants.EUROPEANA_ID)) {
      throw new IllegalArgumentException(
          "SolrQuery fields must either be empty or contain europeana_id");
    }
  }

  /**
   * Cursor functionality requires a sort clause in the query
   *
   * @param solrQuery query object
   */
  private void ensureSortClause(SolrQuery solrQuery) {
    if (CollectionUtils.isEmpty(solrQuery.getSorts())) {
      throw new IllegalArgumentException("SolrQuery must specify a sort with a unique field");
    }
  }
}
