package eu.europeana.fulltext.indexing.processor;

import static eu.europeana.fulltext.indexing.model.IndexingAction.DELETE_DOCUMENT;
import static eu.europeana.fulltext.indexing.model.IndexingAction.UPDATE_METADATA_FIELDS;
import static eu.europeana.fulltext.indexing.model.IndexingAction.UPDATE_FULLTEXT_FIELDS;
import static eu.europeana.fulltext.indexing.model.IndexingAction.WRITE_DOCUMENT;

import eu.europeana.fulltext.indexing.model.AnnoPageRecordId;
import eu.europeana.fulltext.indexing.model.IndexingWrapper;
import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;
import eu.europeana.fulltext.indexing.solr.FulltextSolrService;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class FulltextIndexingWrapperCreator implements ItemProcessor<AnnoPageRecordId, IndexingWrapper> {

  private final IndexingAnnoPageRepository repository;
  private final FulltextSolrService fulltextSolrService;

  public FulltextIndexingWrapperCreator(
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
      // creates a new record with Fulltext and Metadata fields synced
      actionWrapper = new IndexingWrapper(recordId, UPDATE_FULLTEXT_FIELDS,
          UPDATE_METADATA_FIELDS, WRITE_DOCUMENT);
    } else if (active && exists) {
      // record already exists, so we just update the fulltext fields
      actionWrapper = new IndexingWrapper(recordId, UPDATE_FULLTEXT_FIELDS, WRITE_DOCUMENT);
    } else if (!active && exists) {
      // record doesn't exist in Mongo, so we delete from Solr
      actionWrapper = new IndexingWrapper(recordId, DELETE_DOCUMENT);
    }

    return actionWrapper;
  }
}
