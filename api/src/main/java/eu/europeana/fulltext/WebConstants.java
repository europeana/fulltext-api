package eu.europeana.fulltext;

public class WebConstants {

  private WebConstants() {
    // hide implicit public constructor
  }

  public static final String REQUEST_VALUE_DATASET_ID = "datasetId";
  public static final String REQUEST_VALUE_LOCAL_ID = "localId";
  public static final String REQUEST_VALUE_PAGE_ID = "pageId";
  public static final String REQUEST_VALUE_ANNO_ID = "annoId";
  public static final String REQUEST_VALUE_MEDIA = "media";
  public static final String REQUEST_VALUE_LANG = "lang";
  public static final String REQUEST_VALUE_ORIGINAL_LANG = "originalLang";
  public static final String REQUEST_VALUE_RIGHTS = "rights";
  public static final String REQUEST_VALUE_SOURCE = "source";
  public static final String REQUEST_VALUE_DOC = "document";

  public static final String MOTIVATION_SUBTITLING = "subtitling";

  // Fulltext Urls constants
  public static final String PRESENTATION = "presentation";
  public static final String ANNOPAGE = "annopage";
  public static final String ITEM_BASE_URL = "http://data.europeana.eu/item";
  public static final String FULLTEXT_BASE_URL = "http://data.europeana.eu/fulltext";
  public static final String ANNOTATION_BASE_URL = "http://data.europeana.eu/annotation";
  public static final String ANNOPAGE_BASE_URL = "https://iiif.europeana.eu/presentation";
}
