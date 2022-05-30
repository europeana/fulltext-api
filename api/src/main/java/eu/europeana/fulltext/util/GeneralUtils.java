package eu.europeana.fulltext.util;

import eu.europeana.fulltext.WebConstants;
import eu.europeana.fulltext.subtitles.edm.EdmAnnotation;
import eu.europeana.fulltext.subtitles.edm.EdmReference;
import eu.europeana.fulltext.subtitles.edm.EdmTimeBoundary;
import eu.europeana.fulltext.entity.AnnoPage;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

public class GeneralUtils {


  private GeneralUtils() {
    // private constructor to hide implicit one
  }

  /** Matches spring.profiles.active property in test/resource application.properties file */
  public static final String ACTIVE_TEST_PROFILE = "test";


  /**
   * Regex used for validating annotation ids. '%s' will be replaced by allowed domains (via
   * String.format()) when compiling the Pattern.
   */
  public static final String ANNOTATION_ID_REGEX = "https?://" + "%s" + "/annotation/\\d+";

  private static final Pattern ANNOTATION_ID_SUFFIX_PATTERN = Pattern.compile("/annotation/\\d+$");

  /**
   * Generates Annotation ID. Hash of -> annotation.getType() url of the target (mediaUrl +
   * fragment) (if present) url of the fulltextResource (fulltext resource url + fragment)
   *
   * <p>fragment is calculated based on the boundaries. See: {@link EdmTimeBoundary#getFragment()} ()}
   * Or {@link EdmTimeBoundary#getFragment()}
   *
   * @param annotation
   * @return
   */
  public static String generateHash(EdmAnnotation annotation) {
    StringBuilder hashInput = new StringBuilder(annotation.getType().name());
    if (annotation.hasTargets()) {
      EdmReference mr = annotation.getTargets().get(0);
      hashInput.append(mr.getURL());
    }

    hashInput.append(annotation.getTextReference().getURL());

    return DigestUtils.md5Hex(hashInput.toString()).toLowerCase();
  }

  public static String generateHash(String itemID) {
    return DigestUtils.md5Hex(itemID).toLowerCase();
  }

  /**
   * Generates Annotation page url
   *
   * @param itemID
   * @return
   */
  public static String getAnnotationPageURI(String itemID) {
    return "http://data.europeana.eu/annotation" + itemID;
  }

  /**
   * Genrated fulltext Uri
   *
   * @param itemID
   * @param id
   * @return
   */
  public static String getFullTextResourceURI(String itemID, String id) {
    return WebConstants.FULLTEXT_BASE_URL + itemID + "/" + id;
  }

  /**
   * Generates record Id
   *
   * @param datasetId
   * @param localId
   * @return
   */
  public static String generateRecordId(String datasetId, String localId) {
    return "/" + datasetId + "/" + localId;
  }

  public static String getDsId(String recordId) {
    return recordId.split("/")[1];
  }

  public static String getLocalId(String recordId) {
    return recordId.split("/")[2];
  }

  /**
   * Returns the Existing AnnoPage Url
   *
   * @param annoPage
   * @return
   */
  public static String getAnnoPageUrl(AnnoPage annoPage) {
    return "/"
        + WebConstants.PRESENTATION
        + "/"
        + annoPage.getDsId()
        + "/"
        + annoPage.getLcId()
        + "/"
        + WebConstants.ANNOPAGE
        + "/"
        + annoPage.getPgId();
  }

  /**
   * Returns the Existing Translation AnnoPage Url lang parameter is required to fetch the
   * translation annopage
   *
   * @param annoPage
   * @return
   */
  public static String getTranslationAnnoPageUrl(AnnoPage annoPage) {
    if (StringUtils.isNotEmpty(annoPage.getLang())) {
      return getAnnoPageUrl(annoPage)
          + "?"
          + WebConstants.REQUEST_VALUE_LANG
          + "="
          + annoPage.getLang();
    }
    return getAnnoPageUrl(annoPage);
  }

  public static String[] getAnnoPageToString(List<? extends AnnoPage> annoPages) {
    return annoPages.stream().map(AnnoPage::toString).toArray(String[]::new);
  }

  /** Gets the "{dsId}/{lcId}" part from an EntityId string */
  public static String getRecordIdFromUri(String recordUri) {
    // recordUri is always http://data.europeana.eu/item/{dsId}/{lcId}"
    String[] parts = recordUri.split("/");

    return "/" + parts[parts.length - 2] + "/" + parts[parts.length - 1];
  }

  public static boolean isValidAnnotationId(String uri, Predicate<String> pattern) {
    return pattern.test(uri);
  }

  public static String getDeletedEndpoint(String annotationId) {
    // annotation id has form at http://<host>/annotation/18503
    // deletions endpoint is http://<host>/annotations/deleted
    return ANNOTATION_ID_SUFFIX_PATTERN.matcher(annotationId).replaceFirst("/annotations/deleted");
  }

  /**
   * Derives PageID from a media URL.
   *
   * @param mediaUrl media (target) url
   * @return MD5 hash of media url truncated to the first 5 characters
   */
  public static String derivePageId(String mediaUrl) {
    // truncate md5 hash to reduce URL length.
    // Should not be changed as this method can be used in place of fetching the pageId from the
    // database.
    return generateHash(mediaUrl).substring(0, 5);
  }

  public static boolean testProfileNotActive(String activeProfileString) {
    return Arrays.stream(activeProfileString.split(",")).noneMatch(ACTIVE_TEST_PROFILE::equals);
  }

  public static String[] getStringableListToString(List<? extends Object> list){
    return list.stream().map(Object::toString).toArray(String[]::new);

  }

  public static String getTranslationAnnoPageUrl(
      String dsId, String lcId, String pgId, String lang) {
    String path = String.format("/presentation/%s/%s/%s", dsId, lcId, pgId);

    if (StringUtils.isNotEmpty(lang)) {
      path = path + "?lang=" + lang;
    }

    return path;
  }

  public static String generateResourceId(String recordId, String language, String media) {
    return generateHash(recordId + language + media);
  }
}
