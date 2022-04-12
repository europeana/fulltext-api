package eu.europeana.fulltext.subtitles;

import java.util.Arrays;

/** Enum for supported Mime Types along with the handler */
public enum SubtitleType {
  WEB_VTT("text/vtt"),
  SRT("text/plain");

  private final String mimeType;

  SubtitleType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getMimeType() {
    return mimeType;
  }

  /**
   * Gets the SubtitleType for the specified mimeType value
   *
   * @param mimeType mimeType to check for
   * @return SubtitleType instance or null if mimeType isn't supported
   */
  public static SubtitleType getValueByMimetype(String mimeType) {
    return Arrays.stream(SubtitleType.values())
        .filter(v -> v.mimeType.equalsIgnoreCase(mimeType))
        .findFirst()
        .orElse(null);
  }
}
