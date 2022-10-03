package eu.europeana.fulltext.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

public class IntegrationTestUtils {

  public static final String ACCEPT_JSONLD = "application/ld+json";

  public static final String SUBTITLE_VTT = "/subtitles/submission.vtt";
  public static final String SUBTITLE_VTT_2 = "/subtitles/submission_pg2.vtt";

  public static final String  SUBTITLE_MEDIA = "https://www.filmportal.de/sites/default/files/video/Salem06_x264.mp4";
  public static final String  SUBTITLE_DSID = "08604";
  public static final String  SUBTITLE_LCID = "EAAE870171E24F05A64CE364D750631A";

  public static final String  SUBTITLE_2_MEDIA = "http://repozytorium.fn.org.pl/sites/default/files/video/PLa46686d6-9896-48ac-9258-83a63bf3980c/012_mf_143_6-1.mp4";
  public static final String  SUBTITLE_2_DSID = "08609";
  public static final String  SUBTITLE_2_LCID = "a46686d6_9896_48ac_9258_83a63bf3980c";

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
