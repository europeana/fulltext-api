package eu.europeana.fulltext.indexing.processor;

import eu.europeana.fulltext.indexing.IndexingConstants;
import eu.europeana.fulltext.indexing.batch.IndexingAction;
import eu.europeana.fulltext.indexing.batch.IndexingWrapper;
import eu.europeana.fulltext.indexing.model.AnnoPageRecordId;
import eu.europeana.fulltext.util.GeneralUtils;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
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
    String lcId = GeneralUtils.getDsId(europeanaId);

    return new AnnoPageRecordId(dsId, lcId);
  }
}
