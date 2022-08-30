package eu.europeana.fulltext.indexing.reader;

import eu.europeana.fulltext.indexing.IndexingConstants;
import eu.europeana.fulltext.indexing.solr.FulltextSolrService;
import eu.europeana.fulltext.indexing.solr.SolrSearchCursorIterator;
import java.util.Iterator;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.batch.item.data.AbstractPaginatedDataItemReader;

public class FulltextSolrDocumentReader extends AbstractPaginatedDataItemReader<String> {

  private SolrSearchCursorIterator solrIterator;
  private final Logger logger = LogManager.getLogger(FulltextSolrDocumentReader.class);

  private final FulltextSolrService fulltextSolr;

  public FulltextSolrDocumentReader(FulltextSolrService fulltextSolr) {
    this.fulltextSolr = fulltextSolr;
  }

  @Override
  protected Iterator<String> doPageRead() {
    if (solrIterator.hasNext()) {
      SolrDocumentList solrDocuments = solrIterator.next();
      return solrDocuments.stream()
          .map(d -> d.getFieldValue(IndexingConstants.EUROPEANA_ID).toString())
          .iterator();
    }

    return null;
  }

  @Override
  protected void doOpen() throws Exception {
    super.doOpen();
    // Non-restartable, as we expect this to run in multi-threaded steps.
    // see: https://stackoverflow.com/a/20002493
    setSaveState(false);
    setName(FulltextSolrDocumentReader.class.getName());

    if (solrIterator == null) {
      solrIterator = fulltextSolr.createFulltextSyncIterator();
      if (logger.isDebugEnabled()) {
        logger.debug(
            "Created Solr iterator for fetching all documents in Fulltext. Query={}",
            solrIterator.getQuery());
      }
    }
  }
}
