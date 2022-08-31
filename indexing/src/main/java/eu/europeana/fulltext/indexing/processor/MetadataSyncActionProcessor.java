package eu.europeana.fulltext.indexing.processor;

import eu.europeana.fulltext.indexing.model.IndexingAction;
import eu.europeana.fulltext.indexing.model.IndexingWrapper;
import eu.europeana.fulltext.indexing.model.AnnoPageRecordId;
import eu.europeana.fulltext.util.GeneralUtils;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class MetadataSyncActionProcessor implements ItemProcessor<String, IndexingWrapper> {

  @Override
  public IndexingWrapper process(String europeanaId) throws Exception {
    return new IndexingWrapper(IndexingAction.UPDATE, createAnnoPageRecordId(europeanaId));
  }

  private AnnoPageRecordId createAnnoPageRecordId(String europeanaId) {

    String dsId = GeneralUtils.getDsId(europeanaId);
    String lcId = GeneralUtils.getLocalId(europeanaId);

    return new AnnoPageRecordId(dsId, lcId);
  }
}
