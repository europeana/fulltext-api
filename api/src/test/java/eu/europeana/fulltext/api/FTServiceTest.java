package eu.europeana.fulltext.api;

import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.model.FTResource;
import eu.europeana.fulltext.api.model.v2.AnnotationPageV2;
import eu.europeana.fulltext.api.model.v2.AnnotationV2;
import eu.europeana.fulltext.api.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.api.model.v3.AnnotationV3;
import eu.europeana.fulltext.api.service.exception.ResourceDoesNotExistException;
import eu.europeana.fulltext.repository.AnnoPageRepository;
import eu.europeana.fulltext.repository.ResourceRepository;
import eu.europeana.fulltext.api.service.EDM2IIIFMapping;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.api.service.exception.AnnoPageDoesNotExistException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;

import static eu.europeana.fulltext.api.TestUtils.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

/**
 * Created by luthien on 26/09/2018.
 * TODO - add some FTResource handling test cases (prepared two FTResource objects already in the TestUtils class)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = "classpath:fulltext-test.properties")
@SpringBootTest(classes = {FTService.class, FTSettings.class, EDM2IIIFMapping.class})
public class FTServiceTest {


    @Autowired
    private FTService ftService;

    @MockBean
    private AnnoPageRepository apRepository;
    @MockBean
    private ResourceRepository resRepository;


    @Before
    public void setup(){
        given(apRepository.existsByPageId(eq("ds1"), eq("lc1"), eq("pg1")))
                .willReturn(true);
        given(apRepository.findByDatasetLocalPageId(eq("ds1"), eq("lc1"), eq("pg1")))
                .willReturn(anp_1);
        given(apRepository.existsWithAnnoId(eq("ds1"), eq("lc1"), eq("an1")))
                .willReturn(true);
        given(apRepository.findByDatasetLocalAnnoId(eq("ds1"), eq("lc1"), eq("an1")))
                .willReturn(anp_1);
        given(apRepository.existsWithAnnoId(eq("ds1"), eq("lc1"), eq("an2")))
                .willReturn(true);
        given(apRepository.findByDatasetLocalAnnoId(eq("ds1"), eq("lc1"), eq("an2")))
                .willReturn(anp_1);
        given(apRepository.existsWithAnnoId(eq("ds1"), eq("lc1"), eq("an3")))
                .willReturn(true);
        given(apRepository.findByDatasetLocalAnnoId(eq("ds1"), eq("lc1"), eq("an3")))
                .willReturn(anp_1);
        given(resRepository.existsByLimitOne(eq("ds1"), eq("lc1"), eq("res1")))
                .willReturn(true);
        given(resRepository.findByDatasetLocalResId(eq("ds1"), eq("lc1"), eq("res1")))
                .willReturn(res_1);
        given(resRepository.existsByLimitOne(eq("ds1"), eq("lc1"), eq("res2")))
                .willReturn(true);
        given(resRepository.findByDatasetLocalResId(eq("ds1"), eq("lc1"), eq("res2")))
                .willReturn(res_2);
    }



    /**
     * First create an AnnotationPageV2 through the FTService and EDM2IIIFMapping code (with mockito'd entity classes
     * instead of retrieving from Mongo); and deep compare the result with a manually constructed AnnotationPageV2
     * object containing the same identifiers (see TestUtils.java for details)
     */
    @Test
    public void testGetAnnotationPageV2() throws AnnoPageDoesNotExistException {
        prepareAnnotationPageV2();
        AnnotationPageV2 ap = ftService.generateAnnoPageV2(ftService.fetchAnnoPage("ds1", "lc1", "pg1"),false, new ArrayList<>());
        assertReflectionEquals(anpv2_1, ap);
    }

    /**
     * First create an AnnotationPageV3 through the FTService and EDM2IIIFMapping code (with mockito'd entity classes
     * instead of retrieving from Mongo); and deep compare the result with a manually constructed AnnotationPageV3
     * object containing the same identifiers (see TestUtils.java for details)
     */
    @Test
    public void testGetAnnotationPageV3() throws AnnoPageDoesNotExistException {
        prepareAnnotationPageV3();
        AnnotationPageV3 ap = ftService.generateAnnoPageV3(ftService.fetchAnnoPage("ds1", "lc1", "pg1"),false, new ArrayList<>());
        assertReflectionEquals(anpv3_1, ap);
    }

    @Test
    public void testGetAnnotationsV2() throws AnnoPageDoesNotExistException {
        prepareAnnotationsV2();
        AnnotationV2 an = ftService.generateAnnotationV2(ftService.fetchAPAnnotation(
                "ds1", "lc1", "an1"), "an1");
        assertReflectionEquals(annv2_1, an);

        an = ftService.generateAnnotationV2(ftService.fetchAPAnnotation(
                "ds1", "lc1", "an2"), "an2");
        assertReflectionEquals(annv2_2, an);

        an = ftService.generateAnnotationV2(ftService.fetchAPAnnotation(
                "ds1", "lc1", "an3"), "an3");
        assertReflectionEquals(annv2_3, an);
    }

    @Test
    public void testGetAnnotationsV3() throws AnnoPageDoesNotExistException {
        prepareAnnotationsV3();
        AnnotationV3 an = ftService.generateAnnotationV3(ftService.fetchAPAnnotation(
                "ds1", "lc1", "an1"), "an1");
        assertReflectionEquals(annv3_1, an);

        an = ftService.generateAnnotationV3(ftService.fetchAPAnnotation(
                "ds1", "lc1", "an2"), "an2");
        assertReflectionEquals(annv3_2, an);

        an = ftService.generateAnnotationV3(ftService.fetchAPAnnotation(
                "ds1", "lc1", "an3"), "an3");
        assertReflectionEquals(annv3_3, an);
    }

    @Test
    public void testGetResource() throws ResourceDoesNotExistException {
        buildFTResources();
        FTResource ftr = ftService.fetchFTResource(
                "ds1", "lc1", "res1");
        assertReflectionEquals(ftres_1, ftr);

        ftr = ftService.fetchFTResource(
                "ds1", "lc1", "res2");
        assertReflectionEquals(ftres_2, ftr);
    }

}
