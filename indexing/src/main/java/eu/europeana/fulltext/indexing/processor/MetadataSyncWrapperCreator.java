package eu.europeana.fulltext.indexing.processor;

import static eu.europeana.fulltext.util.GeneralUtils.getDsId;
import static eu.europeana.fulltext.util.GeneralUtils.getLocalId;

import eu.europeana.fulltext.indexing.IndexingConstants;
import eu.europeana.fulltext.indexing.model.AnnoPageRecordId;
import eu.europeana.fulltext.indexing.model.IndexingAction;
import eu.europeana.fulltext.indexing.model.IndexingWrapper;
import org.apache.solr.common.SolrDocument;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Processor that only creates an {@link IndexingWrapper} for the Solr document
 */
@Component
public class MetadataSyncWrapperCreator implements ItemProcessor<SolrDocument, IndexingWrapper> {

  @Override
  public IndexingWrapper process(SolrDocument doc) throws Exception {

   String europeanaId = doc.getFieldValue(IndexingConstants.EUROPEANA_ID).toString();

   // simply creates an IndexingWrapper for the document.
    return new IndexingWrapper(createAnnoPageRecordId(europeanaId),
        ProcessorUtils.toSolrInputDocument(doc), IndexingAction.UPDATE_METADATA_FIELDS, IndexingAction.WRITE_DOCUMENT);
  }

  private AnnoPageRecordId createAnnoPageRecordId(String europeanaId) {

    String dsId = getDsId(europeanaId);
    String lcId = getLocalId(europeanaId);

    return new AnnoPageRecordId(dsId, lcId);
  }



}
