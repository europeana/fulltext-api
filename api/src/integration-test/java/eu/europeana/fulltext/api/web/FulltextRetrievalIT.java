package eu.europeana.fulltext.api.web;

import static eu.europeana.fulltext.api.IntegrationTestUtils.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.fulltext.WebConstants;
import eu.europeana.fulltext.api.BaseIntegrationTest;
import eu.europeana.fulltext.api.IntegrationTestUtils;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.subtitles.AnnotationPreview;
import eu.europeana.fulltext.subtitles.FulltextType;
import eu.europeana.fulltext.util.AnnotationUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
class FulltextRetrievalIT extends BaseIntegrationTest {

  private MockMvc mockMvc;

  private AnnoPage subtitleAnnopageOrginal;
  private AnnoPage subtitleAnnopageTransalation_1;
    private AnnoPage subtitleAnnopageTransalation_2;

    private AnnoPage transcriptionAnnoPage;


  @BeforeEach
  void setUp() throws IOException, EuropeanaApiException {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    this.ftService.deleteAll();

    // add original subtitle
    subtitleAnnopageOrginal = ftService.createAnnoPage(AnnotationUtils.createAnnotationPreview(
            SUBTITLE_DSID, SUBTITLE_LCID, "nl", true, "http://creativecommons.org/licenses/by-sa/4.0/", null, SUBTITLE_MEDIA,
            IntegrationTestUtils.loadFile(SUBTITLE_VTT),FulltextType.WEB_VTT), false);

    // add translation subtitle
    subtitleAnnopageTransalation_1 = ftService.createAnnoPage(AnnotationUtils.createAnnotationPreview(
            SUBTITLE_2_DSID, SUBTITLE_2_LCID, "fr", false, "http://creativecommons.org/licenses/by-sa/4.0/", null, SUBTITLE_2_MEDIA,
            IntegrationTestUtils.loadFile(SUBTITLE_VTT_2),FulltextType.WEB_VTT), false);

      // add translation subtitle
      subtitleAnnopageTransalation_2 = ftService.createAnnoPage(AnnotationUtils.createAnnotationPreview(
              SUBTITLE_2_DSID, SUBTITLE_2_LCID, "es", false, "http://creativecommons.org/licenses/by-sa/4.0/", null, SUBTITLE_2_MEDIA,
              IntegrationTestUtils.loadFile(SUBTITLE_VTT_2),FulltextType.WEB_VTT), false);

    // add transcription
    transcriptionAnnoPage = ftService.createAnnoPage(AnnotationUtils.createAnnotationPreview(
            TRANSCRIPTION_DSID, TRANSCRIPTION_LCID, "en", true, "http://creativecommons.org/licenses/by-sa/4.0/", null, TRANSCRIPTION_MEDIA,
            TRANSCRIPTION_CONTENT,FulltextType.PLAIN), false);

    // add them to mongo
      ftService.upsertAnnoPage(List.of(subtitleAnnopageOrginal, subtitleAnnopageTransalation_1, subtitleAnnopageTransalation_2, transcriptionAnnoPage));
  }

  // AnnoPage Info Test
  @Test
  void annoPageInfoTest() throws Exception {
      System.out.println("HERE Mongo DATA ======");

      System.out.println(ftService.countAnnoPage());
      System.out.println(ftService.countResource());

      System.out.println(subtitleAnnopageOrginal.toString());
      System.out.println(subtitleAnnopageTransalation_1.toString());
      System.out.println(subtitleAnnopageTransalation_1.getRes().getValue());

      System.out.println(subtitleAnnopageTransalation_2.toString());


      mockMvc.perform(
            head(
                    "/presentation/{datasetId}/{localId}/annopage",
                    subtitleAnnopageOrginal.getDsId(),
                    subtitleAnnopageOrginal.getLcId()
            ).accept(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk());

      mockMvc.perform(
                      head(
                              "/presentation/{datasetId}/{localId}/annopage",
                              subtitleAnnopageTransalation_1.getDsId(),
                              subtitleAnnopageTransalation_1.getLcId()
                      ).accept(ACCEPT_JSONLD))
              .andDo(MockMvcResultHandlers.print())
              .andExpect(status().isOk());

    mockMvc.perform(
                    head(
                            "/presentation/{datasetId}/{localId}/annopage",
                            transcriptionAnnoPage.getDsId(),
                            transcriptionAnnoPage.getLcId()
                    ).accept(ACCEPT_JSONLD))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk());

    mockMvc.perform(
                    head(
                            "/presentation/{datasetId}/{localId}/annopage",
                            transcriptionAnnoPage.getDsId(),
                            transcriptionAnnoPage.getLcId()
                    ).accept(MediaType.APPLICATION_ATOM_XML))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isNotAcceptable());

    mockMvc.perform(
                    head(
                            "/presentation/{datasetId}/{localId}/annopage",
                            "test",
                            "test"
                    ).accept(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isNotFound());

  }

  //Annopage retreival Test

    @Test
    void annoPageJson_Ok_Test() throws Exception {

      // original lang present
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                subtitleAnnopageOrginal.getDsId(),
                                subtitleAnnopageOrginal.getLcId(),
                                subtitleAnnopageOrginal.getPgId()
                        ).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // original lang present + format 3
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                subtitleAnnopageOrginal.getDsId(),
                                subtitleAnnopageOrginal.getLcId(),
                                subtitleAnnopageOrginal.getPgId()
                        ).param("format", "3")
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // original not present - without lang should return 404
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                subtitleAnnopageTransalation_1.getDsId(),
                                subtitleAnnopageTransalation_1.getLcId(),
                                subtitleAnnopageTransalation_1.getPgId()
                        ).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());

        // original not present - with lang should return 200
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                subtitleAnnopageTransalation_2.getDsId(),
                                subtitleAnnopageTransalation_2.getLcId(),
                                subtitleAnnopageTransalation_2.getPgId()
                        ).param(WebConstants.REQUEST_VALUE_LANG, subtitleAnnopageTransalation_2.getLang())
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // with text granularity
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                subtitleAnnopageOrginal.getDsId(),
                                subtitleAnnopageOrginal.getLcId(),
                                subtitleAnnopageOrginal.getPgId()
                        ).param("textGranularity", "media")
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
  }


    @Test
    void annoPageJson_4xx_Test() throws Exception {
       // annopage not found
        mockMvc.perform(
                        get("/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                "test",
                               "test",
                                "test"
                        ).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());

        // invalid accept header
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                transcriptionAnnoPage.getDsId(),
                                transcriptionAnnoPage.getLcId(),
                                transcriptionAnnoPage.getPgId()
                        ).accept(MediaType.APPLICATION_ATOM_XML))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotAcceptable());

        // invalid version
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                transcriptionAnnoPage.getDsId(),
                                transcriptionAnnoPage.getLcId(),
                                transcriptionAnnoPage.getPgId()
                        ).param("format", "8")
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());

        // invalid text granularity
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                transcriptionAnnoPage.getDsId(),
                                transcriptionAnnoPage.getLcId(),
                                transcriptionAnnoPage.getPgId()
                        ).param("textGranularity", "test")
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }

    // jsonLd annopage retrieval test
    @Test
    void annoPageJsonld_Ok_Test() throws Exception {

        // original lang present
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                subtitleAnnopageOrginal.getDsId(),
                                subtitleAnnopageOrginal.getLcId(),
                                subtitleAnnopageOrginal.getPgId()
                        ).accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // original lang present + format 3
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                subtitleAnnopageOrginal.getDsId(),
                                subtitleAnnopageOrginal.getLcId(),
                                subtitleAnnopageOrginal.getPgId()
                        ).param("format", "3")
                                .accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // original not present - without lang should return 404
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                subtitleAnnopageTransalation_1.getDsId(),
                                subtitleAnnopageTransalation_1.getLcId(),
                                subtitleAnnopageTransalation_1.getPgId()
                        ).accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());

        // original not present - with lang should return 200
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                subtitleAnnopageTransalation_1.getDsId(),
                                subtitleAnnopageTransalation_1.getLcId(),
                                subtitleAnnopageTransalation_1.getPgId()
                        ).param(WebConstants.REQUEST_VALUE_LANG, subtitleAnnopageTransalation_1.getLang())
                                .accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // with text granularity
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                subtitleAnnopageOrginal.getDsId(),
                                subtitleAnnopageOrginal.getLcId(),
                                subtitleAnnopageOrginal.getPgId()
                        ).param("textGranularity", "media")
                                .accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }


    @Test
    void annoPageJsonld_4xx_Test() throws Exception {
        // annopage not found
        mockMvc.perform(
                        get("/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                "test",
                                "test",
                                "test"
                        ).accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());

        // invalid accept header
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                transcriptionAnnoPage.getDsId(),
                                transcriptionAnnoPage.getLcId(),
                                transcriptionAnnoPage.getPgId()
                        ).accept(MediaType.APPLICATION_ATOM_XML))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotAcceptable());

        // invalid version
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                transcriptionAnnoPage.getDsId(),
                                transcriptionAnnoPage.getLcId(),
                                transcriptionAnnoPage.getPgId()
                        ).param("format", "8")
                                .accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());

        // invalid text granularity
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                transcriptionAnnoPage.getDsId(),
                                transcriptionAnnoPage.getLcId(),
                                transcriptionAnnoPage.getPgId()
                        ).param("textGranularity", "test")
                                .accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }

    // annoPageHeadExists json and jsonLd
    @Test
    void annoPageHeadExists_Test() throws Exception {
        // original lang present + jsonld
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                subtitleAnnopageOrginal.getDsId(),
                                subtitleAnnopageOrginal.getLcId(),
                                subtitleAnnopageOrginal.getPgId()
                        ).accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // original lang not present + json
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                subtitleAnnopageTransalation_2.getDsId(),
                                subtitleAnnopageTransalation_2.getLcId(),
                                subtitleAnnopageTransalation_2.getPgId()
                        ).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // annopage not found
        mockMvc.perform(
                        head("/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                "test",
                                "test",
                                "test"
                        ).accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());

        // invalid version format
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                subtitleAnnopageOrginal.getDsId(),
                                subtitleAnnopageOrginal.getLcId(),
                                subtitleAnnopageOrginal.getPgId()
                        )
                                .param("format", "9")
                                .accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }

    // annotationJson test
    //@Test
    void annotationJson_Test() throws Exception {
        // original lang present + jsonld
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                subtitleAnnopageOrginal.getDsId(),
                                subtitleAnnopageOrginal.getLcId(),
                                subtitleAnnopageOrginal.getPgId()
                        ).accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

        // @Test
  void retrievingDeprecatedAnnoPageShouldReturn410() throws Exception {
    AnnoPage annoPage =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), AnnoPage.class);

    ftService.upsertAnnoPage(List.of(annoPage));
    // manually deprecate AnnoPage
    ftService.deprecateAnnoPages(
        annoPage.getDsId(), annoPage.getLcId(), annoPage.getPgId(), annoPage.getLang());

    mockMvc
        .perform(
            get(
                    "/presentation/{dsId}/{lcId}/annopage/{pageId}",
                    annoPage.getDsId(),
                    annoPage.getLcId(),
                    annoPage.getPgId())
                .param(WebConstants.REQUEST_VALUE_LANG, annoPage.getLang())
                .accept(MediaType.APPLICATION_JSON))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isGone());
  }
}
