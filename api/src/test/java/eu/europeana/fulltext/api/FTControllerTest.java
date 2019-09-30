package eu.europeana.fulltext.api;


import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.service.CacheUtils;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.api.service.exception.AnnoPageDoesNotExistException;
import eu.europeana.fulltext.api.service.exception.ResourceDoesNotExistException;
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
import static eu.europeana.fulltext.api.config.FTDefinitions.*;
import static org.hamcrest.CoreMatchers.*;
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
 * TODO add tests for If-Modified-Since header and the CORS headers
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

    private static final String JSON_RES_1_OUTPUT       = "{FTResource_1 : DATA}";
    private static final String JSON_RES_2_OUTPUT       = "{FTResource_2 : DATA}";

    private static final String THE_WRONG_ETAG          = "W/\"abcdef0123456789\"";
    private static final String ANOTHER_WRONG_ETAG      = "W/\"bcdefa1234567890\"";

    private static final String HEADER_ETAG             = "ETag";
    private static final String HEADER_LASTMODIFIED     = "Last-Modified";
    private static final String HEADER_ALLOW            = "Allow";
    private static final String HEADER_CACHECONTROL     = "Cache-Control";
    private static final String HEADER_VARY             = "Vary";
    private static final String HEADER_CONTENTTYPE      = "Content-Type";

    private static final String VALUE_ALLOW             = "GET, HEAD";
    private static final String VALUE_CACHECONTROL      = "no-cache";
    private static final String VALUE_VARY              = "Accept";

    private static final String HEADER_IFNONEMATCH      = "If-None-Match";
    private static final String HEADER_IFMATCH          = "If-Match";
    private static final String HEADER_IFMODIFIEDSINCE  = "If-Modified-Since";

    private static final String ANY                     = "*";
    private static final String HEADER_ACCEPT           = "Accept";
    private static final String LASTMODIFIED_GMT        = "Sun, 22 Feb 2015 23:00:00 GMT";
    private static final String BEGINNINGOFTIME         = "Thu, 11 Jan 1990 00:00:00 GMT";
    private static final String BEFORETHEBEGINNING      = "Wed, 10 Jan 1990 00:00:00 GMT";
    private static final String AFTERTHEBEGINNING       = "Fri, 12 Jan 1990 00:00:00 GMT";


    private static String v2ETag;
    private static String v3ETag;
    private static String multipleETags;
    private static String dikkertjeDapV2ETag;
    private static String multiMrVanZoetenETag;
    private static String littleSillyPetV3ETag;
    private static String dieKuckeBackenWollteETag;
    private static String dasTeigWollteNichtAufgehenETag;
    private static String multiTeigWollteNichtAufgehenETag;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FTService  ftService;
    @MockBean
    private FTSettings ftSettings;
//    @MockBean
//    private CacheUtils cacheUtils;

    @Before
    public void setup() throws AnnoPageDoesNotExistException, SerializationException, ResourceDoesNotExistException {

        given(ftService.fetchAnnoPage(any(), any(), any())).willReturn(anp_1);
        given(ftService.generateAnnoPageV2(anp_1)).willReturn(anpv2_1);
        given(ftService.generateAnnoPageV3(anp_1, false)).willReturn(anpv3_1);
        given(ftService.fetchAPAnnotation(any(), any(), any())).willReturn(anp_1);
        given(ftService.generateAnnotationV2(any(), eq("an1"))).willReturn(annv2_1);
        given(ftService.generateAnnotationV2(any(), eq("an2"))).willReturn(annv2_2);
        given(ftService.generateAnnotationV2(any(), eq("an3"))).willReturn(annv2_3);
        given(ftService.generateAnnotationV3(any(), eq("an1"))).willReturn(annv3_1);
        given(ftService.generateAnnotationV3(any(), eq("an2"))).willReturn(annv3_2);
        given(ftService.generateAnnotationV3(any(), eq("an3"))).willReturn(annv3_3);

        given(ftService.fetchFTResource(any(), any(), eq("res1"))).willReturn(ftres_1);
        given(ftService.fetchFTResource(any(), any(), eq("res2"))).willReturn(ftres_2);

        given(ftService.serialise(anpv2_1)).willReturn(JSONLD_ANP_V2_OUTPUT);
        given(ftService.serialise(annv2_1)).willReturn(JSONLD_ANN_V2_1_OUTPUT);
        given(ftService.serialise(annv2_2)).willReturn(JSONLD_ANN_V2_2_OUTPUT);
        given(ftService.serialise(annv2_3)).willReturn(JSONLD_ANN_V2_3_OUTPUT);
        given(ftService.serialise(anpv3_1)).willReturn(JSONLD_ANP_V3_OUTPUT);
        given(ftService.serialise(annv3_1)).willReturn(JSONLD_ANN_V3_1_OUTPUT);
        given(ftService.serialise(annv3_2)).willReturn(JSONLD_ANN_V3_2_OUTPUT);
        given(ftService.serialise(annv3_3)).willReturn(JSONLD_ANN_V3_3_OUTPUT);

        given(ftService.serialise(ftres_1)).willReturn(JSON_RES_1_OUTPUT);
        given(ftService.serialise(ftres_2)).willReturn(JSON_RES_2_OUTPUT);

        given(ftService.doesAnnoPageExist(any(), any(), startsWith("a"))).willReturn(true);
        given(ftService.doesAnnoPageExist(any(), any(), startsWith("z"))).willReturn(false);

        given(ftSettings.getAppVersion()).willReturn("v1.0-test");
        given(ftService.getSettings()).willReturn(ftSettings);

        v2ETag = CacheUtils.generateETag(
                "bombombom" + "heskoembelge" + "gevettakkegareziet", CacheUtils.dateToZonedUTC(lastModifiedDate),
                "2" + ftSettings.getAppVersion(), true);
        v3ETag = CacheUtils.generateETag(
                "scareamoose" + "willyoudo" + "thedamntango", CacheUtils.dateToZonedUTC(lastModifiedDate),
                "3" + ftSettings.getAppVersion(), true);
        multipleETags = THE_WRONG_ETAG + "," + v2ETag + "," + ANOTHER_WRONG_ETAG + "," + v3ETag;
        dikkertjeDapV2ETag = CacheUtils.generateETag(
                "dikkertjedap" + "zatopdetrap" + "an1", CacheUtils.dateToZonedUTC(lastModifiedDate),
                "2" + ftSettings.getAppVersion(), true);
        multiMrVanZoetenETag = ANOTHER_WRONG_ETAG + "," + CacheUtils.generateETag(
                "zaterdags" + "inhetaquarium" + "an3", CacheUtils.dateToZonedUTC(lastModifiedDate),
                "2" + ftSettings.getAppVersion(), true) + "," +
                               THE_WRONG_ETAG;
        littleSillyPetV3ETag = CacheUtils.generateETag(
                "iseealittle" + "sillypetbesideaman" + "an1", CacheUtils.dateToZonedUTC(lastModifiedDate),
                "3" + ftSettings.getAppVersion(), true);
        dieKuckeBackenWollteETag = CacheUtils.generateSimpleETag(
                "esgabeinefraures1" + "de" + KUCKEBACKENWOLLTE + ftSettings.getAppVersion(), true);
        dasTeigWollteNichtAufgehenETag = CacheUtils.generateSimpleETag(
                "aberdasteigres2" + "de" + WUERDEJANICHTAUFGEHEN + ftSettings.getAppVersion(), true);
        multiTeigWollteNichtAufgehenETag = THE_WRONG_ETAG + "," + dasTeigWollteNichtAufgehenETag + "," + ANOTHER_WRONG_ETAG;
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
                    .andExpect(header().string(HEADER_CONTENTTYPE,
                                               containsString("profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")))
                    .andExpect(content().json(JSONLD_ANP_V2_OUTPUT))
                    .andDo(print());

        this.mockMvc.perform(get("/presentation/tis_een/vreemdeling/annopage/zeeker")
                                     .param("format", "3"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HEADER_CONTENTTYPE,
                                               containsString("profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")))
                    .andExpect(content().json(JSONLD_ANP_V3_OUTPUT))
                    .andDo(print());

        this.mockMvc.perform(get("/presentation/ziet_de/maan_schijnt/annopage/door_den_boomen")
                                     .header("Accept",
                                             "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V2 +" \""))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HEADER_CONTENTTYPE,
                                               containsString("profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")))
                    .andExpect(content().json(JSONLD_ANP_V2_OUTPUT))
                    .andDo(print());

        this.mockMvc.perform(get("/presentation/makkers/staakt_uw/annopage/wild_geraasch")
                                     .header("Accept",
                                             "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V3 + " \""))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HEADER_CONTENTTYPE,
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
                    .andExpect(header().string(HEADER_CONTENTTYPE,
                                               containsString("profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")))
                    .andExpect(content().json(JSONLD_ANN_V2_1_OUTPUT))
                    .andDo(print());

        this.mockMvc.perform(get("/presentation/heydude/dontletmedown/anno/an3").param("format", "3"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HEADER_CONTENTTYPE,
                                               containsString("profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")))
                    .andExpect(content().json(JSONLD_ANN_V3_3_OUTPUT))
                    .andDo(print());

        this.mockMvc.perform(get("/presentation/we_are_the_walrus/kookookechoo/anno/an2")
                                     .param("format", "2"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HEADER_CONTENTTYPE,
                                               containsString("profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")))
                    .andExpect(content().json(JSONLD_ANN_V2_2_OUTPUT))
                    .andDo(print());

        this.mockMvc.perform(get("/presentation/let_me_take_you_down/cause_im_going_to/anno/an3")
                                     .header("Accept", "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V2 + "\""))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HEADER_CONTENTTYPE,
                                               containsString("profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")))
                    .andExpect(content().json(JSONLD_ANN_V2_3_OUTPUT))
                    .andDo(print());

        this.mockMvc.perform(get("/presentation/strawberry_fields/nothing_is_real/anno/an2")
                                     .header("Accept", "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V3 + "\""))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HEADER_CONTENTTYPE,
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

    /**
     * test for the If-None-Match header handling for AnnoPage
     */
    @Test
    public void testIfNoneMatchAnnoPage() throws Exception {

        // matching ETag: should return HTTP 304. Also tests if the controller returns the regular headers
        this.mockMvc.perform(get("/presentation/bombombom/heskoembelge/annopage/gevettakkegareziet")
                                     .header(HEADER_ACCEPT, "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")
                                     .header(HEADER_IFNONEMATCH, v2ETag))
                    .andExpect(header().string(HEADER_ETAG, containsString(v2ETag)))
                    .andExpect(header().string(HEADER_LASTMODIFIED, containsString(LASTMODIFIED_GMT)))
                    .andExpect(header().string(HEADER_ALLOW, containsString(VALUE_ALLOW)))
                    .andExpect(header().string(HEADER_CACHECONTROL, containsString(VALUE_CACHECONTROL)))
                    .andExpect(header().string(HEADER_VARY, containsString(VALUE_VARY)))
                    .andExpect(content().string(""))
                    .andExpect(status().isNotModified())
                    .andDo(print());

        // ditto for "*"
        this.mockMvc.perform(get("/presentation/scareamoose/willyoudo/annopage/thedamntango")
                                     .header(HEADER_ACCEPT, "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")
                                     .header(HEADER_IFNONEMATCH, ANY))
                    .andExpect(content().string(""))
                    .andExpect(status().isNotModified())
                    .andDo(print());

        // and ditto for multiple ETags if it contains the matching one
        this.mockMvc.perform(get("/presentation/bombombom/heskoembelge/annopage/gevettakkegareziet")
                                     .header(HEADER_ACCEPT, "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")
                                     .header(HEADER_IFNONEMATCH, multipleETags))
                    .andExpect(content().string(""))
                    .andExpect(status().isNotModified())
                    .andDo(print());

        // but when the ETag does not match, expect a regular response. Check the regular headers again as well.
        this.mockMvc.perform(get("/presentation/scareamoose/willyoudo/annopage/thedamntango")
                                     .header(HEADER_ACCEPT, "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")
                                     .header(HEADER_IFNONEMATCH, THE_WRONG_ETAG))
                    .andExpect(header().string(HEADER_CONTENTTYPE,
                                           containsString("profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")))
                    .andExpect(header().string(HEADER_ETAG, containsString(v3ETag)))
                    .andExpect(header().string(HEADER_LASTMODIFIED, containsString(LASTMODIFIED_GMT)))
                    .andExpect(header().string(HEADER_ALLOW, containsString(VALUE_ALLOW)))
                    .andExpect(header().string(HEADER_CACHECONTROL, containsString(VALUE_CACHECONTROL)))
                    .andExpect(header().string(HEADER_VARY, containsString(VALUE_VARY)))
                    .andExpect(content().json(JSONLD_ANP_V3_OUTPUT))
                    .andExpect(status().isOk())
                    .andDo(print());

        // and for two nonmatching ETags
        this.mockMvc.perform(get("/presentation/scareamoose/willyoudo/annopage/thedamntango")
                                     .header(HEADER_ACCEPT, "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")
                                     .header(HEADER_IFNONEMATCH, ANOTHER_WRONG_ETAG + "," + THE_WRONG_ETAG))
                    .andExpect(content().json(JSONLD_ANP_V3_OUTPUT))
                    .andExpect(status().isOk())
                    .andDo(print());
    }

    /**
     * test for the If-Match header handling for AnnoPage
     */
    @Test
    public void testIfMatchAnnoPage() throws Exception {

        // matching ETag: should return HTTP 200 + regular JSON content. Also tests if the controller returns the regular headers
        this.mockMvc.perform(get("/presentation/bombombom/heskoembelge/annopage/gevettakkegareziet")
                                     .header(HEADER_ACCEPT, "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")
                                     .header(HEADER_IFMATCH, v2ETag))
                    .andExpect(header().string(HEADER_ETAG, containsString(v2ETag)))
                    .andExpect(header().string(HEADER_LASTMODIFIED, containsString(LASTMODIFIED_GMT)))
                    .andExpect(header().string(HEADER_ALLOW, containsString(VALUE_ALLOW)))
                    .andExpect(header().string(HEADER_CACHECONTROL, containsString(VALUE_CACHECONTROL)))
                    .andExpect(header().string(HEADER_VARY, containsString(VALUE_VARY)))
                    .andExpect(content().json(JSONLD_ANP_V2_OUTPUT))
                    .andExpect(status().isOk())
                    .andDo(print());

        // ditto for "*"
        this.mockMvc.perform(get("/presentation/scareamoose/willyoudo/annopage/thedamntango")
                                     .header(HEADER_ACCEPT, "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")
                                     .header(HEADER_IFMATCH, ANY))
                    .andExpect(content().json(JSONLD_ANP_V3_OUTPUT))
                    .andExpect(status().isOk());

        // and ditto for multiple ETags if it contains the matching one
        this.mockMvc.perform(get("/presentation/bombombom/heskoembelge/annopage/gevettakkegareziet")
                                     .header(HEADER_ACCEPT, "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")
                                     .header(HEADER_IFMATCH, multipleETags))
                    .andExpect(content().json(JSONLD_ANP_V2_OUTPUT))
                    .andExpect(status().isOk());

        // but when the ETag does not match, expect a HTTP 412 without the regular headers.
        this.mockMvc.perform(get("/presentation/scareamoose/willyoudo/annopage/thedamntango")
                                     .header(HEADER_ACCEPT, "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")
                                     .header(HEADER_IFMATCH, THE_WRONG_ETAG))
                    .andExpect(header().string(HEADER_ETAG, nullValue()))
                    .andExpect(header().string(HEADER_LASTMODIFIED, nullValue()))
                    .andExpect(header().string(HEADER_ALLOW, nullValue()))
                    .andExpect(header().string(HEADER_CACHECONTROL, nullValue()))
                    .andExpect(header().string(HEADER_VARY, nullValue()))
                    .andExpect(content().string(""))
                    .andExpect(status().isPreconditionFailed())
                    .andDo(print());
    }

    /**
     * test for the If-None-Match header handling for Annotations
     */
    @Test
    public void testIfNoneMatchAnno() throws Exception {

        // matching ETag: should return HTTP 304. Also tests if the controller returns the regular headers
        this.mockMvc.perform(get("/presentation/dikkertjedap/zatopdetrap/anno/an1")
                                     .header(HEADER_ACCEPT, "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")
                                     .header(HEADER_IFNONEMATCH, dikkertjeDapV2ETag))
                    .andExpect(header().string(HEADER_ETAG, containsString(dikkertjeDapV2ETag)))
                    .andExpect(header().string(HEADER_LASTMODIFIED, containsString(LASTMODIFIED_GMT)))
                    .andExpect(header().string(HEADER_ALLOW, containsString(VALUE_ALLOW)))
                    .andExpect(header().string(HEADER_CACHECONTROL, containsString(VALUE_CACHECONTROL)))
                    .andExpect(header().string(HEADER_VARY, containsString(VALUE_VARY)))
                    .andExpect(content().string(""))
                    .andExpect(status().isNotModified())
                    .andDo(print());

        // ditto for "*"
        this.mockMvc.perform(get("/presentation/meestervanzoeten/wastezijnvoeten/anno/an2")
                                     .header(HEADER_ACCEPT, "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")
                                     .header(HEADER_IFNONEMATCH, ANY))
                    .andExpect(content().string(""))
                    .andExpect(status().isNotModified())
                    .andDo(print());

        // and ditto for multiple ETags if it contains the matching one
        this.mockMvc.perform(get("/presentation/zaterdags/inhetaquarium/anno/an3")
                                     .header(HEADER_ACCEPT, "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")
                                     .header(HEADER_IFNONEMATCH, multiMrVanZoetenETag))
                    .andExpect(content().string(""))
                    .andExpect(status().isNotModified())
                    .andDo(print());

        // but when the ETag does not match, expect a regular response. Check the regular headers again as well.
        this.mockMvc.perform(get("/presentation/iseealittle/sillypetbesideaman/anno/an1")
                                     .header(HEADER_ACCEPT, "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")
                                     .header(HEADER_IFNONEMATCH, THE_WRONG_ETAG))
                    .andExpect(header().string(HEADER_CONTENTTYPE,
                                               containsString("profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")))
                    .andExpect(header().string(HEADER_ETAG, containsString(littleSillyPetV3ETag)))
                    .andExpect(header().string(HEADER_LASTMODIFIED, containsString(LASTMODIFIED_GMT)))
                    .andExpect(header().string(HEADER_ALLOW, containsString(VALUE_ALLOW)))
                    .andExpect(header().string(HEADER_CACHECONTROL, containsString(VALUE_CACHECONTROL)))
                    .andExpect(header().string(HEADER_VARY, containsString(VALUE_VARY)))
                    .andExpect(content().json(JSONLD_ANN_V3_1_OUTPUT))
                    .andExpect(status().isOk())
                    .andDo(print());
    }

    /**
     * test for the If-Match header handling for Annotations
     */
    @Test
    public void testIfMatchAnno() throws Exception {

        // matching ETag: should return HTTP 200. Also tests if the controller returns the regular headers
        this.mockMvc.perform(get("/presentation/dikkertjedap/zatopdetrap/anno/an1")
                                     .header(HEADER_ACCEPT, "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")
                                     .header(HEADER_IFMATCH, dikkertjeDapV2ETag))
                    .andExpect(header().string(HEADER_ETAG, containsString(dikkertjeDapV2ETag)))
                    .andExpect(header().string(HEADER_LASTMODIFIED, containsString(LASTMODIFIED_GMT)))
                    .andExpect(header().string(HEADER_ALLOW, containsString(VALUE_ALLOW)))
                    .andExpect(header().string(HEADER_CACHECONTROL, containsString(VALUE_CACHECONTROL)))
                    .andExpect(header().string(HEADER_VARY, containsString(VALUE_VARY)))
                    .andExpect(content().json(JSONLD_ANN_V2_1_OUTPUT))
                    .andExpect(status().isOk())
                    .andDo(print());

        // ditto for "*"
        this.mockMvc.perform(get("/presentation/meestervanzoeten/wastezijnvoeten/anno/an2")
                                     .header(HEADER_ACCEPT, "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")
                                     .header(HEADER_IFMATCH, ANY))
                    .andExpect(content().json(JSONLD_ANN_V3_2_OUTPUT))
                    .andExpect(status().isOk())
                    .andDo(print());

        // and ditto for multiple ETags if it contains the matching one
        this.mockMvc.perform(get("/presentation/zaterdags/inhetaquarium/anno/an3")
                                     .header(HEADER_ACCEPT, "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")
                                     .header(HEADER_IFMATCH, multiMrVanZoetenETag))
                    .andExpect(content().json(JSONLD_ANN_V2_3_OUTPUT))
                    .andExpect(status().isOk())
                    .andDo(print());

        // but when the ETag does not match, expect a HTTP 412 without regular headers.
        this.mockMvc.perform(get("/presentation/iseealittle/sillypetbesideaman/anno/an1")
                                     .header(HEADER_ACCEPT, "application/ld+json;profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")
                                     .header(HEADER_IFMATCH, THE_WRONG_ETAG))
                    .andExpect(header().string(HEADER_ETAG, nullValue()))
                    .andExpect(header().string(HEADER_LASTMODIFIED, nullValue()))
                    .andExpect(header().string(HEADER_ALLOW, nullValue()))
                    .andExpect(header().string(HEADER_CACHECONTROL, nullValue()))
                    .andExpect(header().string(HEADER_VARY, nullValue()))
                    .andExpect(content().string(""))
                    .andExpect(status().isPreconditionFailed())
                    .andDo(print());
    }

    /**
     * Basic FTResource test
     */
    @Test
    public void testGetFTResource() throws Exception {
        this.mockMvc.perform(get("/presentation/esgab/einefrau/res1"))
                    .andExpect(status().isOk())
                    .andExpect(content().json(JSON_RES_1_OUTPUT))
                    .andDo(print());

        this.mockMvc.perform(get("/presentation/aberdas/teig/res2"))
                    .andExpect(content().json(JSON_RES_2_OUTPUT))
                    .andDo(print());
    }

    /**
     * Test FTResource for returning JSON or JSONLD
     */
    @Test
    public void testGetFTResourceJsonOrLd() throws Exception {
        this.mockMvc.perform(get("/presentation/esgab/einefrau/res1")
                                     .header(HEADER_ACCEPT, "application/ld+json"))
                    .andExpect(header().string(HEADER_CONTENTTYPE, containsString(MEDIA_TYPE_JSONLD)))
                    .andExpect(status().isOk())
                    .andDo(print());

        this.mockMvc.perform(get("/presentation/aberdas/teig/res2")
                                     .header(HEADER_ACCEPT, "application/json"))
                    .andExpect(header().string(HEADER_CONTENTTYPE, containsString(MEDIA_TYPE_JSON)))
                    .andExpect(status().isOk())
                    .andDo(print());
    }

    /**
     * Test FTResource for If-None-Match handling
     */
    @Test
    public void testIfNoneMatchFTResource() throws Exception {

        // matching ETag: should return HTTP 304. Also tests if the controller returns the regular headers
        this.mockMvc.perform(get("/presentation/esgab/einefrau/res1")
                                     .header(HEADER_ACCEPT, "application/json")
                                     .header(HEADER_IFNONEMATCH, dieKuckeBackenWollteETag))
                    .andExpect(header().string(HEADER_ETAG, containsString(dieKuckeBackenWollteETag)))
                    .andExpect(header().string(HEADER_LASTMODIFIED, containsString(BEGINNINGOFTIME)))
                    .andExpect(header().string(HEADER_ALLOW, containsString(VALUE_ALLOW)))
                    .andExpect(header().string(HEADER_CACHECONTROL, containsString(VALUE_CACHECONTROL)))
                    .andExpect(header().string(HEADER_VARY, containsString(VALUE_VARY)))
                    .andExpect(content().string(""))
                    .andExpect(status().isNotModified())
                    .andDo(print());

        // ditto for "*"
        this.mockMvc.perform(get("/presentation/aberdas/teig/res2")
                                     .header(HEADER_ACCEPT, "application/ld+json")
                                     .header(HEADER_IFNONEMATCH, ANY))
                    .andExpect(content().string(""))
                    .andExpect(status().isNotModified())
                    .andDo(print());

        // and ditto for multiple ETags if it contains the matching one
        this.mockMvc.perform(get("/presentation/aberdas/teig/res2")
                                     .header(HEADER_ACCEPT, "application/json")
                                     .header(HEADER_IFNONEMATCH, multiTeigWollteNichtAufgehenETag))
                    .andExpect(header().string(HEADER_ETAG, containsString(dasTeigWollteNichtAufgehenETag)))
                    .andExpect(content().string(""))
                    .andExpect(status().isNotModified())
                    .andDo(print());

        // but when the ETag does not match, expect a regular response. Check the regular headers again as well.
        this.mockMvc.perform(get("/presentation/esgab/einefrau/res1")
                                     .header(HEADER_ACCEPT, "application/json")
                                     .header(HEADER_IFNONEMATCH, THE_WRONG_ETAG))
                    .andExpect(header().string(HEADER_ETAG, containsString(dieKuckeBackenWollteETag)))
                    .andExpect(header().string(HEADER_LASTMODIFIED, containsString(BEGINNINGOFTIME)))
                    .andExpect(header().string(HEADER_ALLOW, containsString(VALUE_ALLOW)))
                    .andExpect(header().string(HEADER_CACHECONTROL, containsString(VALUE_CACHECONTROL)))
                    .andExpect(header().string(HEADER_VARY, containsString(VALUE_VARY)))
                    .andExpect(content().json(JSON_RES_1_OUTPUT))
                    .andExpect(status().isOk())
                    .andDo(print());


        // TWO wrong ETags ... why not?
        this.mockMvc.perform(get("/presentation/aberdas/teig/res2")
                                     .header(HEADER_ACCEPT, "application/ld+json")
                                     .header(HEADER_IFNONEMATCH, ANOTHER_WRONG_ETAG + "," + THE_WRONG_ETAG))
                    .andExpect(content().json(JSON_RES_2_OUTPUT))
                    .andExpect(header().string(HEADER_CONTENTTYPE, containsString(MEDIA_TYPE_JSONLD)))
                    .andExpect(status().isOk())
                    .andDo(print());
    }

    /**
     * Test FTResource for If-Match handling
     */
    @Test
    public void testIfMatchFTResource() throws Exception {

        // matching ETag: should return HTTP 200 + regular JSON content. Also tests if the controller returns the regular headers
        this.mockMvc.perform(get("/presentation/esgab/einefrau/res1")
                                     .header(HEADER_ACCEPT, "application/json")
                                     .header(HEADER_IFMATCH, dieKuckeBackenWollteETag))
                    .andExpect(header().string(HEADER_ETAG, containsString(dieKuckeBackenWollteETag)))
                    .andExpect(header().string(HEADER_LASTMODIFIED, containsString(BEGINNINGOFTIME)))
                    .andExpect(header().string(HEADER_ALLOW, containsString(VALUE_ALLOW)))
                    .andExpect(header().string(HEADER_CACHECONTROL, containsString(VALUE_CACHECONTROL)))
                    .andExpect(header().string(HEADER_VARY, containsString(VALUE_VARY)))
                    .andExpect(content().json(JSON_RES_1_OUTPUT))
                    .andExpect(status().isOk())
                    .andDo(print());

        // ditto for "*"
        this.mockMvc.perform(get("/presentation/aberdas/teig/res2")
                                     .header(HEADER_ACCEPT, "application/json")
                                     .header(HEADER_IFMATCH, ANY))
                    .andExpect(content().json(JSON_RES_2_OUTPUT))
                    .andExpect(status().isOk());

        // and ditto for multiple ETags if it contains the matching one
        this.mockMvc.perform(get("/presentation/aberdas/teig/res2")
                                     .header(HEADER_ACCEPT, "application/json")
                                     .header(HEADER_IFMATCH, multiTeigWollteNichtAufgehenETag))
                    .andExpect(content().json(JSON_RES_2_OUTPUT))
                    .andExpect(status().isOk());

        // but when the ETag does not match, expect a HTTP 412 without the regular headers.
        this.mockMvc.perform(get("/presentation/esgab/einefrau/res1")
                                     .header(HEADER_ACCEPT, "application/ld+json")
                                     .header(HEADER_IFMATCH, THE_WRONG_ETAG))
                    .andExpect(header().string(HEADER_ETAG, nullValue()))
                    .andExpect(header().string(HEADER_LASTMODIFIED, nullValue()))
                    .andExpect(header().string(HEADER_ALLOW, nullValue()))
                    .andExpect(header().string(HEADER_CACHECONTROL, nullValue()))
                    .andExpect(header().string(HEADER_VARY, nullValue()))
                    .andExpect(content().string(""))
                    .andExpect(status().isPreconditionFailed())
                    .andDo(print());
    }

    /**
     * Test FTResource for date modified handling
     */
    @Test
    public void testIfModifiedSinceFTResource() throws Exception {

        // content is changed after date set in if-modified-since: return regular content plus the whole header zoo
        this.mockMvc.perform(get("/presentation/esgab/einefrau/res1")
                                     .header(HEADER_ACCEPT, "application/json")
                                     .header(HEADER_IFMODIFIEDSINCE, BEFORETHEBEGINNING))
                    .andExpect(header().string(HEADER_ETAG, containsString(dieKuckeBackenWollteETag)))
                    .andExpect(header().string(HEADER_LASTMODIFIED, containsString(BEGINNINGOFTIME)))
                    .andExpect(header().string(HEADER_ALLOW, containsString(VALUE_ALLOW)))
                    .andExpect(header().string(HEADER_CACHECONTROL, containsString(VALUE_CACHECONTROL)))
                    .andExpect(header().string(HEADER_VARY, containsString(VALUE_VARY)))
                    .andExpect(content().json(JSON_RES_1_OUTPUT))
                    .andExpect(status().isOk())
                    .andDo(print());

        // content was last changed before date set in if-modified-since: return nothing with status HTTP 304
        this.mockMvc.perform(get("/presentation/aberdas/teig/res2")
                                     .header(HEADER_ACCEPT, "application/json")
                                     .header(HEADER_IFMODIFIEDSINCE, AFTERTHEBEGINNING))
                    .andExpect(header().string(HEADER_ETAG, nullValue()))
                    .andExpect(header().string(HEADER_LASTMODIFIED, nullValue()))
                    .andExpect(header().string(HEADER_ALLOW, nullValue()))
                    .andExpect(header().string(HEADER_CACHECONTROL, nullValue()))
                    .andExpect(header().string(HEADER_VARY, nullValue()))
                    .andExpect(content().string(""))
                    .andExpect(status().isNotModified())
                    .andDo(print());

    }


}
