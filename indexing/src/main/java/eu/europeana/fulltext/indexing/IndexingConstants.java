package eu.europeana.fulltext.indexing;

import eu.europeana.fulltext.indexing.model.IndexingWrapper;
import java.util.List;

public class IndexingConstants {
  public static final String METADATA_SOLR_BEAN = "metadataSolr";
  public static final String FULLTEXT_SOLR_BEAN = "fulltextSolr";

  // Fulltext Collection Constants
  public static final String EUROPEANA_ID = "europeana_id";
  public static final String TIMESTAMP = "timestamp";
  public static final String VERSION = "_version_";
  public static final String TIMESTAMP_UPDATE_METADATA   = "timestamp_update";
  public static final String TIMESTAMP_UPDATE_FULLTEXT   = "timestamp_update_fulltext";
  public static final String FULLTEXT = "fulltext";
  public static final String PROXY_ISSUED = "proxy_dcterms_issued";
  public static final String ISSUED = "issued";
  public static final String IS_FULLTEXT = "is_fulltext";

  // Solr query constants
  public static final String SOLR_QUERY_DEFAULT = "*:*";
  public static final String ALL  = "*";
  public static final String SOLR_QUERY  = "q";
  public static final String SOLR_FL  = "fl";
  public static final String SOLR_FQ  = "fq";
  public static final String SOLR_QT  = "qt";
  public static final String SOLR_SORT  = "sort";
  public static final String SOLR_SORT_ASC  = " asc";
  public static final String SOLR_EXPORT  = "/export";

  // Date formats
  public static final String METADATA_DATE_FORMAT  = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";


  // Bean names
  public static final String BATCH_THREAD_EXECUTOR = "batchThreadExecutor";
  public static final String FULLTEXT_INDEX_JOB = "fulltextIndexJob";

  public static String[] getRecordId(List<? extends IndexingWrapper> list) {
    return list.stream().map(a -> a.getRecordId().toEuropeanaId()).toArray(String[]::new);
  }
}
