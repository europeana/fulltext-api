package eu.europeana.fulltext.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

public class IntegrationTestUtils {

  public static final String SUBTITLE_VTT = "/subtitles/submission.vtt";

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
  public static final String ANNOPAGE_FILMPORTAL_SALEM06_JSON =
      "/annopages/annopage-filmportal-salem06.json";

  public static String loadFile(String resourcePath) throws IOException {
    InputStream resourceAsStream = IntegrationTestUtils.class.getResourceAsStream(resourcePath);

    if (resourceAsStream == null) {
      throw new IOException("Input file could not be loaded");
    }

    return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8).replace("\n", "");
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
