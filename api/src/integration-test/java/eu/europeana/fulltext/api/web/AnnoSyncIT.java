package eu.europeana.fulltext.api.web;

import static eu.europeana.fulltext.api.IntegrationTestUtils.ANNOPAGE_VIMEO_208310501_JSON;
import static eu.europeana.fulltext.api.IntegrationTestUtils.loadFileAndReplaceServerUrl;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.fulltext.WebConstants;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.api.BaseIntegrationTest;
import eu.europeana.fulltext.api.IntegrationTestUtils;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.util.GeneralUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class AnnoSyncIT extends BaseIntegrationTest {

  private static MockWebServer mockAnnotationApi;
  @Autowired ObjectMapper mapper;

  private static String serverBaseUrl;

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired FTService ftService;
  private MockMvc mockMvc;

  private static final List<String> DELETED_ANNOTATION_IDS = new ArrayList<>();

  @BeforeAll
  static void beforeAll() throws IOException {
    mockAnnotationApi = new MockWebServer();

    serverBaseUrl =
        String.format("http://%s:%s", mockAnnotationApi.getHostName(), mockAnnotationApi.getPort());

    mockAnnotationApi.setDispatcher(
        new Dispatcher() {
          // create mapper here, as we can't access the Autowired one from static method
          final ObjectMapper dispatcherMapper = new ObjectMapper();

          @NotNull
          @Override
          public MockResponse dispatch(@NotNull RecordedRequest request)
              throws InterruptedException {

            try {
              String path = request.getPath();
              // can't use String.equals() as path contains wskey param
              assert path != null;
              if (path.startsWith("/annotations/deleted")) {
                List<String> response =
                    DELETED_ANNOTATION_IDS.stream()
                        .map(id -> serverBaseUrl + "/annotation/" + id)
                        .collect(Collectors.toList());
                return new MockResponse()
                    .setBody(dispatcherMapper.writeValueAsString(response))
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
              }
              List<String> pathSegments =
                  Objects.requireNonNull(request.getRequestUrl()).pathSegments();

              if (pathSegments.size() != 2) {
                // Unsupported request path
                return new MockResponse().setResponseCode(404);
              }

              // path is /annotation/<annotationId>
              String annotationId = pathSegments.get(1);

              // 410 returned for  deleted Annotations
              if (DELETED_ANNOTATION_IDS.contains(annotationId)) {
                return new MockResponse().setResponseCode(410);
              }

              try {
                String body =
                    IntegrationTestUtils.loadFileAndReplaceServerUrl(
                        "/annotations/" + annotationId + ".json", serverBaseUrl);
                return new MockResponse()
                    .setBody(body)
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
              } catch (IOException e) {
                // if annotation file cannot be loaded, return 404
                return new MockResponse().setResponseCode(404);
              }
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        });
  }

  @BeforeEach
  void setUp() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    this.ftService.dropCollections();
  }

  @Test
  void annoSyncShouldFetchNewAnnoPage() throws Exception {
    String annotationId = serverBaseUrl + "/annotation/53696";
    // matches language in /resources/annotations/53696.json
    String lang = "es";

    String expectedTgtId = "https://vimeo.com/524898134";
    mockMvc
        .perform(
            post("/fulltext/annosync")
                .param(WebConstants.REQUEST_VALUE_SOURCE, annotationId)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isAccepted())
        .andExpect(
            jsonPath(
                "$.@id",
                endsWith(
                    "/08604/node_1680982/annopage/" + GeneralUtils.derivePageId(expectedTgtId) + "?lang=" + lang)))
        .andExpect(jsonPath("$.language", is(lang)))
        .andExpect(jsonPath("$.source", is(annotationId)));

    // check that AnnoPage is saved in db
    AnnoPage retrievedAnnoPage =
        ftService.getAnnoPageByPgId(
            "08604", "node_1680982", GeneralUtils.derivePageId(expectedTgtId), "es", false);
    Assertions.assertNotNull(retrievedAnnoPage);

    Assertions.assertNotNull(retrievedAnnoPage.getRes());
    // synced AnnoPage resource should have contributed=true
    Assertions.assertTrue(retrievedAnnoPage.getRes().isContributed());
  }

  @Test
  void annoSyncShouldDeleteRemovedAnnoPage() throws Exception {
    String annId = "53707";
    String deletedAnnotation = serverBaseUrl + "/annotation/" + annId;

    // mark annotation as deleted
    DELETED_ANNOTATION_IDS.add(annId);

    // create AnnoPage in DB (source property in JSON matches url in deleted annotations list)
    AnnoPage annoPage =
        mapper.readValue(
            loadFileAndReplaceServerUrl(ANNOPAGE_VIMEO_208310501_JSON, serverBaseUrl),
            AnnoPage.class);
    ftService.upsertAnnoPage(List.of(annoPage));

    mockMvc
        .perform(
            post("/fulltext/annosync")
                .param(WebConstants.REQUEST_VALUE_SOURCE, deletedAnnotation)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status", is("deleted")));
  }
}
