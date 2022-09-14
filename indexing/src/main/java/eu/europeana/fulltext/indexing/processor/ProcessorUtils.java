package eu.europeana.fulltext.indexing.processor;

import static eu.europeana.fulltext.indexing.IndexingConstants.EUROPEANA_ID;
import static eu.europeana.fulltext.indexing.IndexingConstants.IS_FULLTEXT;
import static eu.europeana.fulltext.indexing.IndexingConstants.PROXY_ISSUED;
import static eu.europeana.fulltext.indexing.IndexingConstants.TIMESTAMP;
import static eu.europeana.fulltext.indexing.IndexingConstants.TIMESTAMP_UPDATE_METADATA;
import static eu.europeana.fulltext.indexing.IndexingConstants.VERSION;

import eu.europeana.fulltext.indexing.IndexingConstants;
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

public class ProcessorUtils {

  private ProcessorUtils() {
    // hide default public constructor
  }

  /**
   * Converts a SolrDocument to a SolrInputDocument by copying all the fields
   */
  public static SolrInputDocument toSolrInputDocument(SolrDocument d) {
    SolrInputDocument doc = new SolrInputDocument();

    for (String name : d.getFieldNames()) {
      doc.addField(name, d.getFieldValue(name));
    }

    return doc;
  }


  /**
   * Copies fields from the metadataDoc argument to the destinationDoc
   * @param metadataDoc
   * @param destinationDoc
   * @param europeanaId
   */
  public static void mergeDocs(SolrDocument metadataDoc, SolrInputDocument destinationDoc, String europeanaId) {

    destinationDoc.setField(EUROPEANA_ID, europeanaId);

    for (String field : metadataDoc.getFieldNames()) {
      if (field.equals(PROXY_ISSUED)) {
        Collection<Object> listIssuedDates = metadataDoc.getFieldValues(PROXY_ISSUED);
        List<String> isoDates = new ArrayList<>();
        for (Object d : listIssuedDates) {
          try {
            //accepts only ISO-8601 extended local date format (https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_LOCAL_DATE)
            LocalDate localDate = LocalDate.parse(d.toString());
            ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.of("Z"));
            isoDates.add(zonedDateTime.format(DateTimeFormatter.ISO_INSTANT));
          } catch (DateTimeException e) {
            // do nothing
          }
        }
        if (!isoDates.isEmpty()) {
          destinationDoc.setField(IndexingConstants.ISSUED, Map.of("set", isoDates));
        }
        continue;
      }
      if (field.equals(IS_FULLTEXT)) {
        destinationDoc.setField(field, Map.of("set", true));
        continue;
      }

      // for non-multivalued fields use "set" instead of "add"
      if (field.equals(TIMESTAMP_UPDATE_METADATA)) {
        destinationDoc.setField(field, metadataDoc.getFieldValue(TIMESTAMP_UPDATE_METADATA));
        continue;
      }

      if (!field.equals(EUROPEANA_ID) && !field.equals(TIMESTAMP) && !field.equals(VERSION)) {
        // _version_ and timestamp are automatically added by Solr
        destinationDoc.setField(field, Map.of("set", metadataDoc.getFieldValue(field)));
      }
    }
    }
}
