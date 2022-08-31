package eu.europeana.fulltext.indexing.processor;

import eu.europeana.fulltext.indexing.model.IndexingAction;
import eu.europeana.fulltext.indexing.model.IndexingWrapper;
import eu.europeana.fulltext.indexing.model.AnnoPageRecordId;
import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;
import eu.europeana.fulltext.indexing.solr.FulltextSolrService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
  public class IndexingActionProcessor implements ItemProcessor<AnnoPageRecordId, IndexingWrapper> {

  private final IndexingAnnoPageRepository repository;
  private final FulltextSolrService fulltextSolrService;
  private static final Logger logger = LogManager.getLogger(IndexingActionProcessor.class);


  public IndexingActionProcessor(
      IndexingAnnoPageRepository repository, FulltextSolrService fulltextSolrService) {
    this.repository = repository;
    this.fulltextSolrService = fulltextSolrService;
  }

  @Override
  public IndexingWrapper process(AnnoPageRecordId recordId) throws Exception {
    if (logger.isTraceEnabled()) {
      logger.trace("Creating action for {}", recordId);
    }

    boolean active = repository.existsActive(recordId.getDsId(), recordId.getLcId());
    boolean exists = fulltextSolrService.existsByEuropeanaId(recordId.toEuropeanaId());
    IndexingWrapper actionWrapper = null;

    if (active && !exists) {
      actionWrapper = new IndexingWrapper(IndexingAction.CREATE, recordId);
    } else if (active && exists) {
      actionWrapper = new IndexingWrapper(IndexingAction.UPDATE, recordId);
    } else if (!active && exists) {
      actionWrapper = new IndexingWrapper(IndexingAction.DELETE, recordId);
    }

    return actionWrapper;
  }
}
