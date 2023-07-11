package eu.europeana.fulltext.indexing.writer;

import eu.europeana.fulltext.indexing.config.IndexingAppSettings;
import eu.europeana.fulltext.indexing.model.IndexingAction;
import eu.europeana.fulltext.indexing.model.IndexingWrapper;
import eu.europeana.fulltext.indexing.solr.FulltextSolrService;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class FulltextSolrInsertionWriter implements ItemWriter<IndexingWrapper> {
  private static final Logger LOGGER = LogManager.getLogger(FulltextSolrInsertionWriter.class);

  private final FulltextSolrService solrService;
  private int count = 0;

  private final AtomicLong nextLoggingThreshold;
  private final AtomicLong insertedCount = new AtomicLong();

  private final long loggingInterval;

  public FulltextSolrInsertionWriter(FulltextSolrService solrService, IndexingAppSettings settings) {
    this.solrService = solrService;
    this.loggingInterval = settings.getProgressLoggingInterval();
    this.nextLoggingThreshold = new AtomicLong(loggingInterval);
  }

  @Override
  public void write(List<? extends IndexingWrapper> list) throws Exception {
    // we only write SolrInputDocuments if action is "create" or "update"
    List<SolrInputDocument> docsToWrite =
        list.stream()
            .filter(
                w ->
                    w.getActions().contains(IndexingAction.WRITE_DOCUMENT))
            .map(IndexingWrapper::getSolrDocument)
            .collect(Collectors.toList());

    if (!docsToWrite.isEmpty()) {
      solrService.writeToSolr(docsToWrite);
      count += docsToWrite.size();
      LOGGER.info("Documents written to Solr fulltext: {} ", count);

    }
  }
}
