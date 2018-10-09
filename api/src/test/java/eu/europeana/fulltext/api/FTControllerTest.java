package eu.europeana.fulltext.api;


import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.api.service.exception.AnnoPageDoesNotExistException;
import eu.europeana.fulltext.api.service.exception.SerializationException;
import eu.europeana.fulltext.api.web.FTController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import static eu.europeana.fulltext.api.TestUtils.*;
import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_IIIF_V2;
import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_IIIF_V3;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test the application's controller
 * @author Lúthien
 * Created on 28-02-2018
 */

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(FTController.class)
//@AutoConfigureMockMvc
public class FTControllerTest {


    private static final String JSONLD_ANP_V2_OUTPUT    = "{AnnotationPage_V2 : JSONLD}";
    private static final String JSONLD_ANP_V3_OUTPUT    = "{AnnotationPage_V3 : JSONLD}";
    private static final String JSONLD_ANN_V2_1_OUTPUT  = "{Annotation_V2_1 : JSONLD}";
    private static final String JSONLD_ANN_V2_2_OUTPUT  = "{Annotation_V2_2 : JSONLD}";
    private static final String JSONLD_ANN_V2_3_OUTPUT  = "{Annotation_V2_3 : JSONLD}";
    private static final String JSONLD_ANN_V3_1_OUTPUT  = "{Annotation_V3_1 : JSONLD}";
    private static final String JSONLD_ANN_V3_2_OUTPUT  = "{Annotation_V3_2 : JSONLD}";
    private static final String JSONLD_ANN_V3_3_OUTPUT  = "{Annotation_V3_3 : JSONLD}";


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FTService ftService;

    @Before
    public void setup() throws AnnoPageDoesNotExistException, SerializationException {
        given(ftService.getAnnotationPageV2(any(), any(), any())).willReturn(anpv2_1);
        given(ftService.getAnnotationV2(any(), any(), eq("an1"))).willReturn(annv2_1);
        given(ftService.getAnnotationV2(any(), any(), eq("an2"))).willReturn(annv2_2);
        given(ftService.getAnnotationV2(any(), any(), eq("an3"))).willReturn(annv2_3);

        given(ftService.getAnnotationPageV3(any(), any(), any())).willReturn(anpv3_1);
        given(ftService.getAnnotationV3(any(), any(), eq("an1"))).willReturn(annv3_1);
        given(ftService.getAnnotationV3(any(), any(), eq("an2"))).willReturn(annv3_2);
        given(ftService.getAnnotationV3(any(), any(), eq("an3"))).willReturn(annv3_3);

        given(ftService.serializeResource(anpv2_1)).willReturn(JSONLD_ANP_V2_OUTPUT);
        given(ftService.serializeResource(annv2_1)).willReturn(JSONLD_ANN_V2_1_OUTPUT);
        given(ftService.serializeResource(annv2_2)).willReturn(JSONLD_ANN_V2_2_OUTPUT);
        given(ftService.serializeResource(annv2_3)).willReturn(JSONLD_ANN_V2_3_OUTPUT);
        given(ftService.serializeResource(anpv3_1)).willReturn(JSONLD_ANP_V3_OUTPUT);
        given(ftService.serializeResource(annv3_1)).willReturn(JSONLD_ANN_V3_1_OUTPUT);
        given(ftService.serializeResource(annv3_2)).willReturn(JSONLD_ANN_V3_2_OUTPUT);
        given(ftService.serializeResource(annv3_3)).willReturn(JSONLD_ANN_V3_3_OUTPUT);

        given(ftService.doesAnnoPageExistByLimitOne(any(), any(), startsWith("a"))).willReturn(true);
        given(ftService.doesAnnoPageExistByLimitOne(any(), any(), startsWith("z"))).willReturn(false);
    }

    /**
     * Basic Annotationpage test (no version supplied)
     * Default we expect a v2 Annotationpage
     */
    @Test
    public void testGetAnnopageV2() throws Exception {
//        MvcResult result = this.mockMvc.perform(get("/presentation/dataset_v2/local_v2/annopage/page_v2"))
//                  .andReturn();
        this.mockMvc.perform(get("/presentation/globl/klok/annopage/bogloe"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().json(JSONLD_ANP_V2_OUTPUT));
    }

    /**
     * Basic Annotationpage test: version 2 / 3 requested through either the
     * format GET parameter or through the Accept header
     * We expect a v2 / v3 Annotationpage and a Content-type header of the requested type
     */
    @Test
    public void testGetAnnopage() throws Exception {

        this.mockMvc.perform(get("/presentation/hoort_wie/klopt_daar/annopage/kinderen")
                                     .param("format", "2"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type",
                                               containsString("profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")))
                    .andExpect(content().json(JSONLD_ANP_V2_OUTPUT))
                    .andDo(print());

        this.mockMvc.perform(get("/presentation/tis_een/vreemdeling/annopage/zeeker")
                                     .param("format", "3"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type",
                                               containsString("profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")))
                    .andExpect(content().json(JSONLD_ANP_V3_OUTPUT))
                    .andDo(print());

        this.mockMvc.perform(get("/presentation/ziet_de/maan_schijnt/annopage/door_den_boomen")
                                     .header("Accept",
                                             "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V2 +" \""))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type",
                                               containsString("profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")))
                    .andExpect(content().json(JSONLD_ANP_V2_OUTPUT))
                    .andDo(print());

        this.mockMvc.perform(get("/presentation/makkers/staakt_uw/annopage/wild_geraasch")
                                     .header("Accept",
                                             "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V3 +" \""))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type",
                                               containsString("profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")))
                    .andExpect(content().json(JSONLD_ANP_V3_OUTPUT))
                    .andDo(print());
    }

    /**
     * Ask for a specific Annotation: default & V2 & V3; requested through either the
     * format GET parameter or through the Accept header
     */
    @Test
    public void testGetAnnotations() throws Exception {

        this.mockMvc.perform(get("/presentation/iwazzawokking/downdastreet/anno/an1"))
                    .andExpect(header().string("Content-Type",
                                               containsString("profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")))
                    .andExpect(content().json(JSONLD_ANN_V2_1_OUTPUT))
                    .andDo(print());

        this.mockMvc.perform(get("/presentation/heydude/dontletmedown/anno/an3").param("format", "3"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type",
                                               containsString("profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")))
                    .andExpect(content().json(JSONLD_ANN_V3_3_OUTPUT))
                    .andDo(print());

        this.mockMvc.perform(get("/presentation/we_are_the_walrus/kookookechoo/anno/an2")
                                     .param("format", "2"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type",
                                               containsString("profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")))
                    .andExpect(content().json(JSONLD_ANN_V2_2_OUTPUT))
                    .andDo(print());

        this.mockMvc.perform(get("/presentation/let_me_take_you_down/cause_im_going_to/anno/an3")
                                     .header("Accept", "application/ld+json;profile=\""+MEDIA_TYPE_IIIF_V2+"\""))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type",
                                               containsString("profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")))
                    .andExpect(content().json(JSONLD_ANN_V2_3_OUTPUT))
                    .andDo(print());

        this.mockMvc.perform(get("/presentation/strawberry_fields/nothing_is_real/anno/an2")
                                     .header("Accept", "application/ld+json;profile=\""+MEDIA_TYPE_IIIF_V3+"\""))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type",
                                               containsString("profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")))
                    .andExpect(content().json(JSONLD_ANN_V3_2_OUTPUT))
                    .andDo(print());
    }

    /**
     * break stuff with a wrong endpoint
     * expected to return a HTTP 404
     * TODO should be extended as soon as we implement HTTP error statuses
     */
    @Test
    public void testErrorResponse() throws Exception {
        this.mockMvc.perform(get("/presentation/wahwahpedaal/prikstok/paprika/zakdoek").param("format", "3"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
    }

    /**
     * test record-exist HEAD call
     *
     */
    @Test
    public void testDoesAnnoPageExist() throws Exception {
        this.mockMvc.perform(head("/presentation/vrolijk/versierde/annopage/annopagetaart"))
                    .andDo(print())
                    .andExpect(status().isOk());

        this.mockMvc.perform(head("/presentation/tergend/zanikende/annopage/zuurpruimen"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
    }



}
