package eu.europeana.fulltext.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

public class IntegrationTestUtils {

  public static final String ACCEPT_JSONLD = "application/ld+json";

  public static final String SUBTITLE_VTT = "/subtitles/submission.vtt";
  public static final String SUBTITLE_VTT_2 = "/subtitles/submission_pg2.vtt";

  public static final String  SUBTITLE_MEDIA = "https://www.filmportal.de/node/1197365";
  public static final String  SUBTITLE_DSID = "08604";
  public static final String  SUBTITLE_LCID = "FDE2205EEE384218A8D986E5138F9691";

  public static final String  SUBTITLE_2_MEDIA = "https://www.filmportal.de/node/1197365";
  public static final String  SUBTITLE_2_DSID = "67546";
  public static final String  SUBTITLE_2_LCID = "ABC2205EEE384218A8D986E5138F9691";

  public static final String  TRANSCRIPTION_MEDIA = "https://europeana1914-1918.s3.amazonaws.com/attachments/261100/21892.261100.original.jpg";
  public static final String  TRANSCRIPTION_DSID = "2020601";
  public static final String  TRANSCRIPTION_LCID = "https___1914_1918_europeana_eu_contributions_21892";
  public static final String  TRANSCRIPTION_CONTENT = "leere Seite";

  public static final String ANNOPAGE_FILMPORTAL_1197365_JSON =
      "/annopages/annopage-filmportal-1197365.json";
  public static final String ANNOPAGE_FILMPORTAL_1197365_EN_JSON =
      "/annopages/annopage-filmportal-1197365_en.json";
  public static final String ANNOPAGE_REPOZYTORIUM_8333_JSON =
      "/annopages/annopage-repozytorium-8333.json";
  public static final String ANNOPAGE_REPOZYTORIUM_9927_JSON =
      "/annopages/annopage-repozytorium-9927.json";
  public static final String ANNOPAGE_VIMEO_208310501_JSON =
      "/annopages/annopage-vimeo-208310501.json";

  public static String loadFile(String resourcePath) throws IOException {
    InputStream resourceAsStream = IntegrationTestUtils.class.getResourceAsStream(resourcePath);

    if (resourceAsStream == null) {
      throw new IOException("Input file could not be loaded");
    }

    return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
  }

  /**
   * Method for loading JSON files, and ensuring annotationId matches mockServer url. Replaces
   * "http://test-annotation-host" in JSON
   */
  public static String loadFileAndReplaceServerUrl(String resourcePath, String replacementString)
      throws IOException {
    return loadFile(resourcePath).replaceFirst("http://test-annotation-host", replacementString);
  }
}
