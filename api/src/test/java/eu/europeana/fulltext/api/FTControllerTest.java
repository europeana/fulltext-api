package eu.europeana.fulltext.api;


import eu.europeana.fulltext.api.model.v2.AnnotationBodyV2;
import eu.europeana.fulltext.api.model.v2.AnnotationFullBodyV2;
import eu.europeana.fulltext.api.model.v2.AnnotationPageV2;
import eu.europeana.fulltext.api.model.v2.AnnotationV2;
import eu.europeana.fulltext.api.model.v3.AnnotationBodyV3;
import eu.europeana.fulltext.api.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.api.model.v3.AnnotationV3;
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
import org.springframework.test.web.servlet.MockMvc;

import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_EDM_JSONLD;
import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_IIIF_V2;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test the application's controller
 * @author Lúthien
 * Created on 28-02-2018
 */

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(FTController.class)
//@AutoConfigureMockMvc
public class FTControllerTest {

    private static final String IIIFBASEURL = "https://iiif.europeana.eu/presentation/";
    private static final String FTBASEURL = "http://data.europeana.eu/fulltext/";


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
        AnnotationBodyV2    anb_01 = createAnnotationBodyV2("anb_01");
        AnnotationBodyV2    anb_02 = createAnnotationFullBodyV2("anb_02", FTBASEURL + "dataset_v2/local_v2/fulltext_v2", "en");
        AnnotationBodyV2    anb_03 = createAnnotationBodyV2("anb_03");
        AnnotationV2        ann_01 = createAnnotationV2("ann_01", anb_01,
                                      new String[]{IIIFBASEURL + "dataset_v2/local_v2/canvas/page_v2#xywh=60,100,30,14"},
                                      "W", "sc:painting");
        AnnotationV2        ann_02 = createAnnotationV2("ann_02", anb_02,
                                      new String[]{IIIFBASEURL + "dataset_v2/local_v2/canvas/page_v2#xywh=95,102,53,15"},
                                      "W", "sc:painting");
        AnnotationV2        ann_03 = createAnnotationV2("ann_03", anb_03,
                                      new String[]{IIIFBASEURL + "dataset_v2/local_v2/canvas/page_v2#xywh=60,100,400,18"},
                                      "L", "sc:painting");
        AnnotationV2[]      ans_01 = new AnnotationV2[] {ann_01, ann_02, ann_03};
        AnnotationPageV2    anp_01 = createAnnotationPageV2("anp_01", ans_01);

        AnnotationBodyV3    anb_11 = createAnnotationBodyV3("anb_11");
        AnnotationBodyV3    anb_12 = createAnnotationBodyV3("anb_12", FTBASEURL + "dataset_v3/local_v3/fulltext_v3", "en");
        AnnotationBodyV3    anb_13 = createAnnotationBodyV3("anb_13");
        AnnotationV3        ann_11 = createAnnotationV3("ann_11", anb_11,
                                                        new String[]{IIIFBASEURL + "dataset_v3/local_v3/canvas/page_v3#xywh=64,97,54,16"},
                                                        "W", "sc:painting");
        AnnotationV3        ann_12 = createAnnotationV3("ann_12", anb_12,
                                                        new String[]{IIIFBASEURL + "dataset_v3/local_v3/canvas/page_v3#xywh=119,95,29,17"},
                                                        "W", "sc:painting");
        AnnotationV3        ann_13 = createAnnotationV3("ann_13", anb_13,
                                                        new String[]{IIIFBASEURL + "dataset_v3/local_v3/canvas/page_v3#xywh=60,96,407,19",
                                                                IIIFBASEURL + "dataset_v3/local_v3/canvas/page_v3#xywh=59,138,133,25"},
                                                        "L", "sc:painting");
        AnnotationV3[]      ans_11 = new AnnotationV3[] {ann_11, ann_12, ann_13};
        AnnotationPageV3    anp_11 = createAnnotationPageV3("anp_11", ans_11);

        given(ftService.getAnnotationPageV2(any(), any(), any())).willReturn(anp_01);
        given(ftService.getAnnotationV2(any(), any(), eq("an1"))).willReturn(ann_01);
        given(ftService.getAnnotationV2(any(), any(), eq("an2"))).willReturn(ann_02);
        given(ftService.getAnnotationV2(any(), any(), eq("an3"))).willReturn(ann_03);

        given(ftService.getAnnotationPageV3(any(), any(), any())).willReturn(anp_11);
        given(ftService.getAnnotationV3(any(), any(), eq("an1"))).willReturn(ann_11);
        given(ftService.getAnnotationV3(any(), any(), eq("an2"))).willReturn(ann_12);
        given(ftService.getAnnotationV3(any(), any(), eq("an3"))).willReturn(ann_13);

        given(ftService.serializeResource(anp_01)).willReturn(JSONLD_ANP_V2_OUTPUT);
        given(ftService.serializeResource(ann_01)).willReturn(JSONLD_ANN_V2_1_OUTPUT);
        given(ftService.serializeResource(ann_02)).willReturn(JSONLD_ANN_V2_2_OUTPUT);
        given(ftService.serializeResource(ann_03)).willReturn(JSONLD_ANN_V2_3_OUTPUT);
        given(ftService.serializeResource(anp_11)).willReturn(JSONLD_ANP_V3_OUTPUT);
        given(ftService.serializeResource(ann_11)).willReturn(JSONLD_ANN_V3_1_OUTPUT);
        given(ftService.serializeResource(ann_12)).willReturn(JSONLD_ANN_V3_2_OUTPUT);
        given(ftService.serializeResource(ann_13)).willReturn(JSONLD_ANN_V3_3_OUTPUT);

        given(ftService.doesAnnoPageExist_exists(any(), any(), startsWith("a"))).willReturn(true);
        given(ftService.doesAnnoPageExist_exists(any(), any(), startsWith("z"))).willReturn(false);

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
     * Basic Annotationpage test: version 2 / 3 asked
     * We expect a v2 / v3 Annotationpage
     */
    @Test
    public void testGetAnnopageV3() throws Exception {
        this.mockMvc.perform(get("/presentation/hoortwie/kloptdaar/annopage/kinderen").param("format", "3"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().json(JSONLD_ANP_V3_OUTPUT));
        this.mockMvc.perform(get("/presentation/tis_een/vreemdeling/annopage/zeker").param("format", "2"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().json(JSONLD_ANP_V2_OUTPUT));
    }

    /**
     * Ask for a specific Annotation, default V2 & V3
     *
     */
    @Test
    public void testGetAnnotations() throws Exception {
        this.mockMvc.perform(get("/presentation/iwazzawokking/downdastreet/anno/an1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().json(JSONLD_ANN_V2_1_OUTPUT));

        this.mockMvc.perform(get("/presentation/heydude/dontletmedown/anno/an3").param("format", "3"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().json(JSONLD_ANN_V3_3_OUTPUT));

        this.mockMvc.perform(get("/presentation/we_are_the_walrus/kookookechoo/anno/an2").param("format", "2"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().json(JSONLD_ANN_V2_2_OUTPUT));
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




    private AnnotationPageV2 createAnnotationPageV2(String id, AnnotationV2[] resources){
        AnnotationPageV2 anp = new AnnotationPageV2(id);
        anp.setResources(resources);
        return anp;
    }

    private AnnotationBodyV2 createAnnotationBodyV2(String id){
        return new AnnotationBodyV2(id);
    }

    private AnnotationFullBodyV2 createAnnotationFullBodyV2(String id, String full, String language){
        AnnotationFullBodyV2 anb = new AnnotationFullBodyV2(id);
        anb.setFull(full);
        anb.setLanguage(language);
        return anb;
    }

    private AnnotationV2 createAnnotationV2(String id, AnnotationBodyV2 resource, String[] on, String dcType, String motivation) {
        AnnotationV2 ann = new AnnotationV2(id);
        ann.setContext(new String[]{MEDIA_TYPE_IIIF_V2, MEDIA_TYPE_EDM_JSONLD});
        ann.setResource(resource);
        ann.setOn(on);
        ann.setDcType(dcType);
        ann.setMotivation(motivation);
        return ann;
    }

    private AnnotationPageV3 createAnnotationPageV3(String id, AnnotationV3[] items){
        AnnotationPageV3 anp = new AnnotationPageV3(id);
        anp.setItems(items);
        return anp;
    }

    private AnnotationBodyV3 createAnnotationBodyV3(String id){
        return new AnnotationBodyV3(id);
    }

    private AnnotationBodyV3 createAnnotationBodyV3(String id, String source, String language){
        AnnotationBodyV3 anb = new AnnotationBodyV3(id, "SpecificResource");
        anb.setSource(source);
        anb.setLanguage(language);
        return anb;
    }

    private AnnotationV3 createAnnotationV3(String id, AnnotationBodyV3 body, String[] target, String dcType, String motivation) {
        AnnotationV3 ann = new AnnotationV3(id);
        ann.setContext(new String[]{MEDIA_TYPE_IIIF_V2, MEDIA_TYPE_EDM_JSONLD});
        ann.setBody(body);
        ann.setTarget(target);
        ann.setDcType(dcType);
        ann.setMotivation(motivation);
        return ann;
    }

}
