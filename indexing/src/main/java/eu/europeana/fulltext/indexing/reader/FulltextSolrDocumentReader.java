package eu.europeana.fulltext.indexing.reader;

import eu.europeana.fulltext.indexing.IndexingConstants;
import eu.europeana.fulltext.indexing.solr.FulltextSolrService;
import eu.europeana.fulltext.indexing.solr.SolrSearchCursorIterator;
import java.util.Collections;
import java.util.Iterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.batch.item.data.AbstractPaginatedDataItemReader;

/**
 * Reader for fetching documents from Fulltext Solr.
 */
public class FulltextSolrDocumentReader extends AbstractPaginatedDataItemReader<SolrDocument> {

  private SolrSearchCursorIterator solrIterator;
  private final Logger logger = LogManager.getLogger(FulltextSolrDocumentReader.class);

  private final FulltextSolrService fulltextSolr;

  public FulltextSolrDocumentReader(FulltextSolrService fulltextSolr) {
    this.fulltextSolr = fulltextSolr;
  }

  @Override
  protected Iterator<SolrDocument> doPageRead() {
    if (solrIterator.hasNext()) {
      return solrIterator.next().iterator();
    }

    return Collections.emptyIterator();
  }

  @Override
  protected void doOpen() throws Exception {
    super.doOpen();
    // Non-restartable, as we expect this to run in multi-threaded steps.
    // see: https://stackoverflow.com/a/20002493
    setSaveState(false);
    setName(FulltextSolrDocumentReader.class.getName());

    if (solrIterator == null) {
      // only europeana_id and timestamp_update are required by the metadata sync workflow
      solrIterator = fulltextSolr.createFulltextSyncIterator(IndexingConstants.EUROPEANA_ID, IndexingConstants.TIMESTAMP_UPDATE_METADATA);
      if (logger.isDebugEnabled()) {
        logger.debug(
            "Created Solr iterator for fetching all documents in Fulltext. Query={}",
            solrIterator.getQuery());
      }
    }
  }
}
