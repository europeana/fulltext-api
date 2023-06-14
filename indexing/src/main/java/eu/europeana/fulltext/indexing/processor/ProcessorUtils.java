package eu.europeana.fulltext.indexing.processor;

import eu.europeana.fulltext.exception.SolrServiceException;
import eu.europeana.fulltext.indexing.IndexingConstants;

import java.net.ProxySelector;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import eu.europeana.fulltext.indexing.solr.FulltextSolrService;
import eu.europeana.fulltext.indexing.solr.MetadataSolrService;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import static eu.europeana.fulltext.indexing.IndexingConstants.*;

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
  public static void mergeDocs(SolrDocument metadataDoc, SolrInputDocument destinationDoc, String europeanaId, FulltextSolrService fulltextSolrService) throws SolrServiceException {

    destinationDoc.setField(EUROPEANA_ID, europeanaId);
    HashSet<String> metadataFields = new HashSet<>(metadataDoc.getFieldNames());
    HashSet<String> fulltextFields = new HashSet<>(fulltextSolrService.getDocument(europeanaId).getFieldNames());

    fulltextFields.removeAll(metadataFields);
    for (String field: fulltextFields) { //if metadata field in fulltext was in the metadata but it no longer is, proceeds to remove it from fulltext
      if (!field.equals(PROXY_ISSUED) && !field.equals(TIMESTAMP_UPDATE_FULLTEXT) && !field.startsWith(FULLTEXT) && !field.equals(IS_FULLTEXT)) {
        destinationDoc.setField(field, Map.of("removeregex", ".*"));         //atomic removal
      }
    }

    for (String field: metadataFields) { //update/add metadata fields in fulltext
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
          destinationDoc.addField(IndexingConstants.ISSUED, Map.of("set", isoDates));
        }

        destinationDoc.addField(field, Map.of("set", metadataDoc.getFieldValue(field)));
        continue;
      }
      if (field.equals(IS_FULLTEXT)) {
        destinationDoc.addField(field, Map.of("set", true));
        continue;
      }

      // for non-multivalued fields use "set" instead of "add"
      if (field.equals(TIMESTAMP_UPDATE_METADATA)) {
        destinationDoc.setField(field, metadataDoc.getFieldValue(TIMESTAMP_UPDATE_METADATA));
        continue;
      }

      if (!field.equals(EUROPEANA_ID) && !field.equals(TIMESTAMP) && !field.equals(VERSION)) {
        // _version_ and timestamp are automatically added by Solr
        destinationDoc.addField(field, Map.of("set", metadataDoc.getFieldValue(field)));
      }
    }
  }

}
