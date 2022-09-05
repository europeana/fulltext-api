package eu.europeana.fulltext.subtitles;

import java.util.Arrays;

/** Enum for supported Mime Types along with the handler */
public enum FulltextType {
  WEB_VTT("text/vtt"),
  SUB_RIP("application/x-subrip"),
  TTML("application/ttml+xml"),
  PLAIN("text/plain");

  private final String mimeType;

  FulltextType(String mimeType) {
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
  public static FulltextType getValueByMimetype(String mimeType) {
    return Arrays.stream(FulltextType.values())
        .filter(v -> v.mimeType.equalsIgnoreCase(mimeType))
        .findFirst()
        .orElse(null);
  }
}
