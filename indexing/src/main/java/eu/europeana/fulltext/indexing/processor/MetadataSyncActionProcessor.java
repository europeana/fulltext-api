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
    // if Metadata has changed, setting "CREATE" here means the existing data is overwritten
    // in Fulltext Solr and recreated
    return new IndexingWrapper(IndexingAction.CREATE, createAnnoPageRecordId(europeanaId));
  }

  private AnnoPageRecordId createAnnoPageRecordId(String europeanaId) {

    String dsId = GeneralUtils.getDsId(europeanaId);
    String lcId = GeneralUtils.getLocalId(europeanaId);

    return new AnnoPageRecordId(dsId, lcId);
  }
}
