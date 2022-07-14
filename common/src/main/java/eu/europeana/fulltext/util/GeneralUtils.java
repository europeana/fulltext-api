package eu.europeana.fulltext.util;

import eu.europeana.fulltext.WebConstants;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

public class GeneralUtils {

  public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXX";
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
   * Creates a hash for the specified annotation.
   * This should normally be used for deriving the id of the annotation.
   * @param annotation annotation to generate a hash for
   * @param lang language of AnnoPage containing this Annotation
   * @param tgtId media url of AnnoPage containing this Annotation
   * @return hashed String for annotation
   */
  public static String createAnnotationHash(Annotation annotation, String tgtId, String lang) {
    StringBuilder input =
        new StringBuilder(tgtId)
            .append(annotation.getDcType())
            .append(lang)
            .append(annotation.getFrom())
            .append(annotation.getTo());

    if (!CollectionUtils.isEmpty(annotation.getTgs())) {
      if (annotation.isMedia()) {
        input.append(
            annotation.getTgs().stream()
                .map(t -> t.getStart() + String.valueOf(t.getEnd()))
                .collect(Collectors.joining()));
      } else {
        input.append(
            annotation.getTgs().stream()
                .map(t -> t.getY() + t.getY() + t.getW() + String.valueOf(t.getH()))
                .collect(Collectors.joining()));
      }
    }

    return generateHash(input.toString());
  }

  public static String generateHash(String itemString) {
    return DigestUtils.md5Hex(itemString).toLowerCase();
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

  public static String getAnnoPageUrl(
      String dsId, String lcId, String pgId, String lang) {
    String path = String.format("/presentation/%s/%s/%s", dsId, lcId, pgId);

    if (StringUtils.isNotEmpty(lang)) {
      path = path + "?lang=" + lang;
    }

    return path;
  }


  public static String generateAnnotationSearchQuery(@Nullable Instant from, @NonNull Instant to) {
    /*
     * if 'from' is null, fetch from the earliest representable time
     */

    String fromString = from != null ? toSolrDateString(from) : "*";
    String toString = toSolrDateString(to);

    return "generated:[" + fromString + " TO " + toString + "] AND  (motivation:subtitling)";
  }

  private static String toSolrDateString(Instant instant) {
    /*
     * escape colons in dates, as the colon is a special character to Solr's parser
     * See: https://solr.apache.org/guide/6_6/working-with-dates.html#WorkingwithDates-DateFormatting
     */
    return instant.atZone(ZoneOffset.UTC).toString().replace(":", "\\:");
  }

  public static String generateResourceId(String recordId, String language, String media) {
    return generateHash(recordId + language + media);
  }


  public static String[] getAnnoPageObjectIds(List<? extends AnnoPage> annoPages) {
    return annoPages.stream().map(a -> a.getDbId().toString()).toArray(String[]::new);
  }
}
