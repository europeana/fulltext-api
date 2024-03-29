package eu.europeana.fulltext.indexing.processor;

import static eu.europeana.fulltext.indexing.IndexingConstants.TIMESTAMP_UPDATE_METADATA;
import static eu.europeana.fulltext.indexing.processor.ProcessorUtils.mergeDocs;

import eu.europeana.fulltext.indexing.model.IndexingAction;
import eu.europeana.fulltext.indexing.model.IndexingWrapper;
import eu.europeana.fulltext.indexing.solr.FulltextSolrService;
import eu.europeana.fulltext.indexing.solr.MetadataSolrService;
import java.util.Date;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.stereotype.Component;

/**
 * Updates metadata fields from the Metadata Solr collection
 */
@Component
public class IndexingMetadataSyncProcessor
    extends BaseIndexingWrapperProcessor {

  private static final Logger LOGGER = LogManager.getLogger(IndexingMetadataSyncProcessor.class);
  private final MetadataSolrService metadataSolr;
  private final FulltextSolrService fulltextSolrService;


  public IndexingMetadataSyncProcessor(MetadataSolrService metadataSolr, FulltextSolrService fulltextSolrService) {
    super(IndexingAction.UPDATE_METADATA_FIELDS);
    this.metadataSolr = metadataSolr;
    this.fulltextSolrService = fulltextSolrService;
  }

  @Override
  protected IndexingWrapper doProcessing(IndexingWrapper indexingWrapper) throws Exception {
    String europeanaId = indexingWrapper.getRecordId().toEuropeanaId();
    // check if document exists on Metadata Collection.
    SolrDocument metadataSolrDocument = metadataSolr.getDocument(europeanaId);
    if (metadataSolrDocument == null || metadataSolrDocument.isEmpty()) {
      if(LOGGER.isDebugEnabled()){
        LOGGER.debug("{} does not exist in metadata collection. Will delete document in fulltext solr", europeanaId);
      }
      indexingWrapper.markForDeletion();
      return indexingWrapper;
    }

    Date fulltextSolrTimestamp =
        indexingWrapper.getSolrDocument().containsKey(TIMESTAMP_UPDATE_METADATA) ?
            (Date) indexingWrapper.getSolrDocument().getFieldValue(TIMESTAMP_UPDATE_METADATA) : null;

    Date metadataSolrTimestamp = (Date)
        metadataSolrDocument.getFieldValue(TIMESTAMP_UPDATE_METADATA);

    if(fulltextSolrTimestamp != null && !metadataSolrTimestamp.after(fulltextSolrTimestamp)){
      if(LOGGER.isDebugEnabled()){
        LOGGER.debug("{} timestamp_update in metadata collection not after fulltext collection value; document not updated" , europeanaId);
      }

      // This means the record isn't passed on to subsequent processors or writers
      return null;
    }

      SolrInputDocument fulltextDoc = indexingWrapper.getSolrDocument();

    // merge fields from Fulltext and Metadata docs
      mergeDocs(metadataSolrDocument, fulltextDoc, europeanaId, fulltextSolrService);
    return indexingWrapper;
  }
}
