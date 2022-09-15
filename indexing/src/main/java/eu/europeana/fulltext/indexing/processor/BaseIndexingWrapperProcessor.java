package eu.europeana.fulltext.indexing.processor;

import eu.europeana.fulltext.indexing.model.IndexingAction;
import eu.europeana.fulltext.indexing.model.IndexingWrapper;
import java.util.Set;
import org.springframework.batch.item.ItemProcessor;

public abstract  class BaseIndexingWrapperProcessor implements
    ItemProcessor<IndexingWrapper, IndexingWrapper> {

  private final IndexingAction processorAction;

  protected BaseIndexingWrapperProcessor(IndexingAction processorAction) {
    this.processorAction = processorAction;
  }

  protected boolean shouldProcess(Set<IndexingAction> indexingActions){
    return indexingActions.contains(processorAction);
  }


  @Override
  public IndexingWrapper process(IndexingWrapper indexingWrapper) throws Exception {
    if(shouldProcess(indexingWrapper.getActions())){
      return doProcessing(indexingWrapper);
    }

    return indexingWrapper;
  }

  protected abstract IndexingWrapper doProcessing(IndexingWrapper indexingWrapper) throws Exception;
}
