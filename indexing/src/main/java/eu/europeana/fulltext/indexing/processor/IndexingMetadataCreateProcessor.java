package eu.europeana.fulltext.indexing.processor;

import static eu.europeana.fulltext.indexing.IndexingConstants.EUROPEANA_ID;
import static eu.europeana.fulltext.indexing.IndexingConstants.ISSUED;
import static eu.europeana.fulltext.indexing.IndexingConstants.IS_FULLTEXT;
import static eu.europeana.fulltext.indexing.IndexingConstants.PROXY_ISSUED;
import static eu.europeana.fulltext.indexing.IndexingConstants.TIMESTAMP;
import static eu.europeana.fulltext.indexing.IndexingConstants.VERSION;

import eu.europeana.fulltext.exception.SolrDocumentException;
import eu.europeana.fulltext.indexing.model.IndexingAction;
import eu.europeana.fulltext.indexing.model.IndexingWrapper;
import eu.europeana.fulltext.indexing.solr.MetadataSolrService;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Retrieves data from MetadataSolr
 */
@Component
public class IndexingMetadataCreateProcessor
    implements ItemProcessor<IndexingWrapper, IndexingWrapper> {

  private final MetadataSolrService metadataSolr;

  public IndexingMetadataCreateProcessor(MetadataSolrService metadataSolr) {
    this.metadataSolr = metadataSolr;
  }

  @Override
  public IndexingWrapper process(IndexingWrapper indexingWrapper) throws Exception {
    // Processor only runs on "Create" action
    if (!indexingWrapper.getAction().equals(IndexingAction.CREATE)) {
      return indexingWrapper;
    }

    String europeanaId = indexingWrapper.getRecordId().toEuropeanaId();
    // check if document exists on Metadata Collection.
    SolrDocument existingDocument = metadataSolr.getDocument(europeanaId);
    if (existingDocument == null || existingDocument.isEmpty()) {
      throw new SolrDocumentException(europeanaId + " does not exist in metadata collection");
    }

    SolrInputDocument fulltextDoc = indexingWrapper.getSolrDocument();

    // merge fields from Fulltext and Metadata docs
    mergeDocs(existingDocument, fulltextDoc, europeanaId);
    return indexingWrapper;
  }

  private void mergeDocs(SolrDocument metadaDoc, SolrInputDocument fulltextDoc, String europeanaId)
      throws SolrDocumentException {

    fulltextDoc.setField(EUROPEANA_ID, europeanaId);

    for (String field : metadaDoc.getFieldNames()) {
      if (field.equals(PROXY_ISSUED)) {
        Collection<Object> listIssuedDates = metadaDoc.getFieldValues(PROXY_ISSUED);
        List<String> isoDates = new ArrayList<>();
        for (Object d : listIssuedDates) {
          try {
            // accepts only ISO-8601 extended local date format
            // (https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_LOCAL_DATE)
            LocalDate localDate = LocalDate.parse(d.toString());
            ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.of("Z"));
            isoDates.add(zonedDateTime.format(DateTimeFormatter.ISO_INSTANT));
          } catch (DateTimeException e) {
            throw new SolrDocumentException(
                String.format("Not parsable date in record  %s : %s", europeanaId, d));
          }
          fulltextDoc.addField(ISSUED, Map.of("set", isoDates));
        }
      }
      if (field.equals(IS_FULLTEXT)) {
        fulltextDoc.addField(field, Map.of("set", true));
      }
      if (!field.equals(EUROPEANA_ID)
          && !field.equals(TIMESTAMP)
          && !field.equals(VERSION)
          && !field.equals(IS_FULLTEXT)) {
        // _version_ and timestamp are automatically added by Solr
        fulltextDoc.addField(field, Map.of("set", metadaDoc.getFieldValue(field)));
      }
    }
  }
}
