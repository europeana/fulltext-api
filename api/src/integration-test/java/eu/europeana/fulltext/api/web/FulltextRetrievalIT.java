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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
  private AnnoPage subtitleAnnopageTransalation;
  private AnnoPage transcriptionAnnoPage;


  @BeforeAll
  void setUp() throws IOException, EuropeanaApiException {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    this.ftService.deleteAll();

    // add original subtitle
    subtitleAnnopageOrginal = ftService.createAnnoPage(AnnotationUtils.createAnnotationPreview(
            SUBTITLE_DSID, SUBTITLE_LCID, "nl", true, "http://creativecommons.org/licenses/by-sa/4.0/", null, SUBTITLE_MEDIA,
            IntegrationTestUtils.loadFile(SUBTITLE_VTT),FulltextType.WEB_VTT), false);

    // add translation subtitle
    subtitleAnnopageTransalation = ftService.createAnnoPage(AnnotationUtils.createAnnotationPreview(
            SUBTITLE_DSID, SUBTITLE_LCID, "fr", true, "http://creativecommons.org/licenses/by-sa/4.0/", null, SUBTITLE_MEDIA,
            IntegrationTestUtils.loadFile(SUBTITLE_VTT),FulltextType.WEB_VTT), false);

    // add transcription
    transcriptionAnnoPage = ftService.createAnnoPage(AnnotationUtils.createAnnotationPreview(
            TRANSCRIPTION_DSID, TRANSCRIPTION_LCID, "en", true, "http://creativecommons.org/licenses/by-sa/4.0/", null, TRANSCRIPTION_MEDIA,
            TRANSCRIPTION_CONTENT,FulltextType.PLAIN), false);
  }

  @AfterAll
  void clear() {
      this.ftService.deleteAll();
  }

  // AnnoPage Info Test
  @Test
  void annoPageInfoTest() throws Exception {
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
                            transcriptionAnnoPage.getDsId(),
                            transcriptionAnnoPage.getLcId()
                    ).accept(eu.europeana.api.commons.web.http.HttpHeaders.CONTENT_TYPE_JSONLD))
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
