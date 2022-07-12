package eu.europeana.fulltext.migrations;

import org.apache.commons.codec.digest.DigestUtils;

public class MigrationUtils {

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

  public static String generateHash(String itemID) {
    return DigestUtils.md5Hex(itemID).toLowerCase();
  }

  public static String generateResourceId(String recordId, String language, String media) {
    return generateHash(recordId + language + media);
  }

  public static String getRecordId(String datasetId, String localId) {
    return "/" + datasetId + "/" + localId;
  }
}
