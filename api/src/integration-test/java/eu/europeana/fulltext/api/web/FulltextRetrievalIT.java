package eu.europeana.fulltext.api.web;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.fulltext.WebConstants;
import eu.europeana.fulltext.api.BaseIntegrationTest;
import eu.europeana.fulltext.api.IntegrationTestUtils;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.subtitles.FulltextType;
import eu.europeana.fulltext.util.AnnotationUtils;
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

import static eu.europeana.fulltext.api.IntegrationTestUtils.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

        // create original subtitle
        subtitleAnnopageOrginal = ftService.createAnnoPage(AnnotationUtils.createAnnotationPreview(
                SUBTITLE_DSID, SUBTITLE_LCID, "nl", true, "http://creativecommons.org/licenses/by-sa/4.0/", null, SUBTITLE_MEDIA,
                IntegrationTestUtils.loadFile(SUBTITLE_VTT), FulltextType.WEB_VTT), false);

        // create translation subtitle
        subtitleAnnopageTransalation_1 = ftService.createAnnoPage(AnnotationUtils.createAnnotationPreview(
                SUBTITLE_2_DSID, SUBTITLE_2_LCID, "fr", false, "http://creativecommons.org/licenses/by-sa/4.0/", null, SUBTITLE_2_MEDIA,
                IntegrationTestUtils.loadFile(SUBTITLE_VTT_2), FulltextType.WEB_VTT), false);

        // create translation subtitle
        subtitleAnnopageTransalation_2 = ftService.createAnnoPage(AnnotationUtils.createAnnotationPreview(
                SUBTITLE_2_DSID, SUBTITLE_2_LCID, "es", false, "http://creativecommons.org/licenses/by-sa/4.0/", null, SUBTITLE_2_MEDIA,
                IntegrationTestUtils.loadFile(SUBTITLE_VTT_2), FulltextType.WEB_VTT), false);

        // create transcription
        transcriptionAnnoPage = ftService.createAnnoPage(AnnotationUtils.createAnnotationPreview(
                TRANSCRIPTION_DSID, TRANSCRIPTION_LCID, "en", true, "http://creativecommons.org/licenses/by-sa/4.0/", null, TRANSCRIPTION_MEDIA,
                TRANSCRIPTION_CONTENT, FulltextType.PLAIN), false);

        // add all annopages to mongo
        ftService.upsertAnnoPage(List.of(subtitleAnnopageOrginal, subtitleAnnopageTransalation_1, subtitleAnnopageTransalation_2, transcriptionAnnoPage));
    }

    // ANNOPAGE INFO TEST
    @Test
    void annoPageInfo_Json_OriginalPresent_OK_Test() throws Exception {
        // with original
        mockMvc.perform(
                        head(
                                "/presentation/{datasetId}/{localId}/annopage",
                                subtitleAnnopageOrginal.getDsId(),
                                subtitleAnnopageOrginal.getLcId()
                        ).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @Test
    void annoPageInfo_Json_Translation_OK_Test() throws Exception {
        // with translations and no original present
        mockMvc.perform(
                        head(
                                "/presentation/{datasetId}/{localId}/annopage",
                                subtitleAnnopageTransalation_1.getDsId(),
                                subtitleAnnopageTransalation_1.getLcId()
                        ).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @Test
    void annoPageInfo_Jsonld_Translation_OK_Test() throws Exception {
        // with original
        mockMvc.perform(
                        head(
                                "/presentation/{datasetId}/{localId}/annopage",
                                subtitleAnnopageTransalation_1.getDsId(),
                                subtitleAnnopageTransalation_1.getLcId()
                        ).accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @Test
    void annoPageInfo_Jsonld_OriginalPresent_OK_Test() throws Exception {
        // with translations and no original present
        mockMvc.perform(
                        head(
                                "/presentation/{datasetId}/{localId}/annopage",
                                transcriptionAnnoPage.getDsId(),
                                transcriptionAnnoPage.getLcId()
                        ).accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

    }

    @Test
    void annoPageInfo_NotAcceptable_Test() throws Exception {
        mockMvc.perform(
                        head(
                                "/presentation/{datasetId}/{localId}/annopage",
                                transcriptionAnnoPage.getDsId(),
                                transcriptionAnnoPage.getLcId()
                        ).accept(MediaType.APPLICATION_ATOM_XML))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void annoPageInfo_NotFound_Test() throws Exception {
        mockMvc.perform(
                        head(
                                "/presentation/{datasetId}/{localId}/annopage",
                                "test",
                                "test"
                        ).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());

    }

    // ANNOPAGE RETRIEVAL TESTS
    @Test
    void annoPageJson_OriginalPresent_Ok_Test() throws Exception {
        // original lang present + format
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
    }


    @Test
    void annoPageJson_OriginalNotPresent_WithoutLang_Test() throws Exception {
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
    }

    @Test
    void annoPageJson_OriginalNotPresent_WithLang_Test() throws Exception {
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
    }

    @Test
    void annoPageJson_WithTextGranularity_Test() throws Exception {
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
    void annoPageJson_Original_DeprecatedAnnoPageShouldReturn410() throws Exception {
        // manually deprecate transcriptionAnoopage
        ftService.deprecateAnnoPages(
                transcriptionAnnoPage.getDsId(), transcriptionAnnoPage.getLcId(), transcriptionAnnoPage.getPgId(), transcriptionAnnoPage.getLang());

        mockMvc
                .perform(
                        get(
                                "/presentation/{dsId}/{lcId}/annopage/{pageId}",
                                transcriptionAnnoPage.getDsId(),
                                transcriptionAnnoPage.getLcId(),
                                transcriptionAnnoPage.getPgId())
                                .param(WebConstants.REQUEST_VALUE_LANG, transcriptionAnnoPage.getLang())
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isGone());
    }

    @Test
    void annoPageJson_Translation_DeprecatedAnnoPageShouldReturn410() throws Exception {
        // manually deprecate translation annopage
        ftService.deprecateAnnoPages(
                subtitleAnnopageTransalation_1.getDsId(), subtitleAnnopageTransalation_1.getLcId(), subtitleAnnopageTransalation_1.getPgId(), subtitleAnnopageTransalation_1.getLang());

        mockMvc
                .perform(
                        get(
                                "/presentation/{dsId}/{lcId}/annopage/{pageId}",
                                subtitleAnnopageTransalation_1.getDsId(),
                                subtitleAnnopageTransalation_1.getLcId(),
                                subtitleAnnopageTransalation_1.getPgId())
                                .param(WebConstants.REQUEST_VALUE_LANG, subtitleAnnopageTransalation_1.getLang())
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isGone());
    }

    @Test
    void annoPageJson_NotFound_Test() throws Exception {
        // annopage not found
        mockMvc.perform(
                        get("/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                "test",
                                "test",
                                "test"
                        ).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());
    }

    @Test
    void annoPageJson_NotAcceptable_Test() throws Exception {
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
    }

    @Test
    void annoPageJson_InvalidFormat_Test() throws Exception {
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

    }

    @Test
    void annoPageJson_InvalidTextGranularity_Test() throws Exception {
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

    // JSONLD ANNO PAGE RETRIEVAL TESTS

    @Test
    void annoPageJsonLd_OriginalPresent_Ok_Test() throws Exception {
        // original lang present
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
    }


    @Test
    void annoPageJsonLd_NoOriginalPresent_WithoutLang_Test() throws Exception {
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
    }

    @Test
    void annoPageJsonLd_NoOriginalPresent_WithLang_Test() throws Exception {
        // original not present - with lang should return 200
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                subtitleAnnopageTransalation_2.getDsId(),
                                subtitleAnnopageTransalation_2.getLcId(),
                                subtitleAnnopageTransalation_2.getPgId()
                        ).param(WebConstants.REQUEST_VALUE_LANG, subtitleAnnopageTransalation_2.getLang())
                                .accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @Test
    void annoPageJsonLd_WithTextGranularity_Test() throws Exception {
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
    void annoPageJsonld_NotAcceptable_Test() throws Exception {
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
    }


    // annoPageHeadExists json and jsonLd
    @Test
    void annoPageHeadExistsJsonLd_Original_Ok_Test() throws Exception {
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

    @Test
    void annoPageHeadExistsJson_OriginalNotPresent_Test() throws Exception {
        // original lang not present + json
        // must return 404 without lang param
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                subtitleAnnopageTransalation_2.getDsId(),
                                subtitleAnnopageTransalation_2.getLcId(),
                                subtitleAnnopageTransalation_2.getPgId()
                        ).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());
    }

    @Test
    void annoPageHeadExistsJson_OriginalNotPresentWithLang_Ok_Test() throws Exception {
        // original lang not present + json
        // must return 200 with lang param
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                subtitleAnnopageTransalation_2.getDsId(),
                                subtitleAnnopageTransalation_2.getLcId(),
                                subtitleAnnopageTransalation_2.getPgId()
                        )
                                .param(WebConstants.REQUEST_VALUE_LANG, "es")
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @Test
    void annoPageHeadExistsJsonLd_NotFound_Ok_Test() throws Exception {
        // annopage not found
        mockMvc.perform(
                        head("/presentation/{datasetId}/{localId}/annopage/{pageId}",
                                "test",
                                "test",
                                "test"
                        ).accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());
    }

    @Test
    void annoPageHeadExistsJsonLd_InvalidFormat_Ok_Test() throws Exception {
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
    @Test
    void annotationJson_Original_Success_Test() throws Exception {
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/anno/{annoID}",
                                subtitleAnnopageOrginal.getDsId(),
                                subtitleAnnopageOrginal.getLcId(),
                                subtitleAnnopageOrginal.getAns().get(1)
                        )
                                .param("format", "2")
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @Test
    void annotationJsonld_Transcription_Success_Test() throws Exception {
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/anno/{annoID}",
                                transcriptionAnnoPage.getDsId(),
                                transcriptionAnnoPage.getLcId(),
                                transcriptionAnnoPage.getAns().get(0)
                        )
                                .param("format", "3")
                                .accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @Test
    void annotationJsonld_Translation_Success_Test() throws Exception {
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/anno/{annoID}",
                                subtitleAnnopageTransalation_2.getDsId(),
                                subtitleAnnopageTransalation_2.getLcId(),
                                subtitleAnnopageTransalation_2.getAns().get(2)
                        )
                                .param("format", "3")
                                .accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }


    @Test
    void annotationJsonld_NotFound_Test() throws Exception {
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/anno/{annoID}",
                                subtitleAnnopageOrginal.getDsId(),
                                subtitleAnnopageOrginal.getLcId(),
                                "test"
                        ).accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());
    }

    @Test
    void annotationJson_NotFound_Test() throws Exception {
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/anno/{annoID}",
                                subtitleAnnopageTransalation_2.getDsId(),
                                subtitleAnnopageTransalation_2.getLcId(),
                                "test"
                        ).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());
    }

    @Test
    void annotationJson_NotAcceptable_Test() throws Exception {
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/anno/{annoID}",
                                subtitleAnnopageOrginal.getDsId(),
                                subtitleAnnopageOrginal.getLcId(),
                                subtitleAnnopageOrginal.getAns().get(0)
                        ).accept(MediaType.APPLICATION_ATOM_XML))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void annotationJson_InvalidFormat_Test() throws Exception {
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/anno/{annoID}",
                                subtitleAnnopageOrginal.getDsId(),
                                subtitleAnnopageOrginal.getLcId(),
                                subtitleAnnopageOrginal.getAns().get(1)
                        )
                                .param("format", "8")
                                .accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }


    // RESOURCE JSON + JSONLD TESTS

    @Test
    void resourceJsonLd_Original_Ok_Test() throws Exception {
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/{pageId}",
                                subtitleAnnopageOrginal.getDsId(),
                                subtitleAnnopageOrginal.getLcId(),
                                subtitleAnnopageOrginal.getPgId()
                        ).accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @Test
    void resourceJson_Original_Ok_Test() throws Exception {
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/{pageId}",
                                transcriptionAnnoPage.getDsId(),
                                transcriptionAnnoPage.getLcId(),
                                transcriptionAnnoPage.getPgId()
                        ).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @Test
    void resourceJsonld_OriginalNotPresent_WithoutLang_Test() throws Exception {
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/{pageId}",
                                subtitleAnnopageTransalation_1.getDsId(),
                                subtitleAnnopageTransalation_1.getLcId(),
                                subtitleAnnopageTransalation_1.getPgId()
                        ).accept(ACCEPT_JSONLD))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void resourceJson_OriginalNotPresent_WithLang_Ok_Test() throws Exception {
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/{pageId}",
                                subtitleAnnopageTransalation_1.getDsId(),
                                subtitleAnnopageTransalation_1.getLcId(),
                                subtitleAnnopageTransalation_1.getPgId()
                        )
                                .param(WebConstants.REQUEST_VALUE_LANG, "es")
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @Test
    void resourceJson_OriginalNotPresent_WithLang_NotFound_Test() throws Exception {
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/{pageId}",
                                subtitleAnnopageTransalation_1.getDsId(),
                                subtitleAnnopageTransalation_1.getLcId(),
                                subtitleAnnopageTransalation_1.getPgId()
                        )
                                .param(WebConstants.REQUEST_VALUE_LANG, "en")
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());
    }

    @Test
    void resourceJson_NotAcceptable_Test() throws Exception {
        mockMvc.perform(
                        get(
                                "/presentation/{datasetId}/{localId}/{pageId}",
                                transcriptionAnnoPage.getDsId(),
                                transcriptionAnnoPage.getLcId(),
                                transcriptionAnnoPage.getPgId()
                        )
                                .param(WebConstants.REQUEST_VALUE_LANG, "en")
                                .accept(MediaType.APPLICATION_ATOM_XML))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotAcceptable());
    }
}
