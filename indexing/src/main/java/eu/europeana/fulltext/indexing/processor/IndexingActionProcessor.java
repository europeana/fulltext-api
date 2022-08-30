package eu.europeana.fulltext.indexing.processor;

import eu.europeana.fulltext.indexing.batch.IndexingAction;
import eu.europeana.fulltext.indexing.batch.IndexingWrapper;
import eu.europeana.fulltext.indexing.model.AnnoPageRecordId;
import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;
import eu.europeana.fulltext.indexing.solr.FulltextSolrService;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
  public class IndexingActionProcessor implements ItemProcessor<AnnoPageRecordId, IndexingWrapper> {

  private final IndexingAnnoPageRepository repository;
  private final FulltextSolrService fulltextSolrService;

  public IndexingActionProcessor(
      IndexingAnnoPageRepository repository, FulltextSolrService fulltextSolrService) {
    this.repository = repository;
    this.fulltextSolrService = fulltextSolrService;
  }

  @Override
  public IndexingWrapper process(AnnoPageRecordId recordId) throws Exception {
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
