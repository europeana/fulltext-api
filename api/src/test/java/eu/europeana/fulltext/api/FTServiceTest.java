/*
 * Copyright 2007-2018 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 *
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */

package eu.europeana.fulltext.api;

import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.model.v2.AnnotationPageV2;
import eu.europeana.fulltext.api.model.v2.AnnotationV2;
import eu.europeana.fulltext.api.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.api.model.v3.AnnotationV3;
import eu.europeana.fulltext.api.repository.impl.AnnoPageRepositoryImpl;
import eu.europeana.fulltext.api.repository.impl.ResourceRepositoryImpl;
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

import java.util.Collections;

import static eu.europeana.fulltext.api.TestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

/**
 * Created by luthien on 26/09/2018.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = "classpath:fulltext-test.properties")
@SpringBootTest(classes = {FTService.class, FTSettings.class, EDM2IIIFMapping.class})
public class FTServiceTest {


    @Autowired
    private FTService ftService;

    @MockBean
    private AnnoPageRepositoryImpl apRepository;
    @MockBean
    private ResourceRepositoryImpl resRepository;


    @Before
    public void setup(){
        given(apRepository.existsByLimitOne(eq("dataset_1"), eq("local_1"), eq("page_1")))
                .willReturn(true);
        given(apRepository.findByDatasetLocalAndPageId(eq("dataset_1"), eq("local_1"), eq("page_1")))
                .willReturn(anp_1);
//                .willReturn(Collections.singletonList(anp_1));
        given(apRepository.existsWithAnnoId(eq("dataset_1"), eq("local_1"), eq("anno_1")))
                .willReturn(true);
        given(apRepository.findByDatasetLocalAndAnnoId(eq("dataset_1"), eq("local_1"), eq("anno_1")))
                .willReturn(anp_1);
//                .willReturn(Collections.singletonList(anp_1));
        given(apRepository.existsWithAnnoId(eq("dataset_1"), eq("local_1"), eq("anno_2")))
                .willReturn(true);
        given(apRepository.findByDatasetLocalAndAnnoId(eq("dataset_1"), eq("local_1"), eq("anno_2")))
                .willReturn(anp_1);
//                .willReturn(Collections.singletonList(anp_1));
        given(apRepository.existsWithAnnoId(eq("dataset_1"), eq("local_1"), eq("anno_3")))
                .willReturn(true);
        given(apRepository.findByDatasetLocalAndAnnoId(eq("dataset_1"), eq("local_1"), eq("anno_3")))
                .willReturn(anp_1);
//                .willReturn(Collections.singletonList(anp_1));
    }



    /**
     * First create an AnnotationPageV2 through the FTService and EDM2IIIFMapping code (with mockito'd entity classes
     * instead of retrieving from Mongo); and deep compare the result with a manually constructed AnnotationPageV2
     * object containing the same identifiers (see TestUtils.java for details)
     */
    @Test
    public void testGetAnnotationPageV2() throws AnnoPageDoesNotExistException {
        prepareAnnotationPageV2();
        AnnotationPageV2 ap = ftService.getAnnotationPageV2(
                "dataset_1", "local_1", "page_1", true);
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
        AnnotationPageV3 ap = ftService.getAnnotationPageV3(
                "dataset_1", "local_1", "page_1", true);
        assertReflectionEquals(anpv3_1, ap);
    }

    @Test
    public void testGetAnnotationsV2() throws AnnoPageDoesNotExistException {
        prepareAnnotationsV2();
        AnnotationV2 an = ftService.getAnnotationV2(
                "dataset_1", "local_1", "anno_1", true);
        assertReflectionEquals(annv2_1, an);

        an = ftService.getAnnotationV2(
                "dataset_1", "local_1", "anno_2", true);
        assertReflectionEquals(annv2_2, an);

        an = ftService.getAnnotationV2(
                "dataset_1", "local_1", "anno_3", true);
        assertReflectionEquals(annv2_3, an);
    }

    @Test
    public void testGetAnnotationsV3() throws AnnoPageDoesNotExistException {
        prepareAnnotationsV3();
        AnnotationV3 an = ftService.getAnnotationV3(
                "dataset_1", "local_1", "anno_1", true);
        assertReflectionEquals(annv3_1, an);

        an = ftService.getAnnotationV3(
                "dataset_1", "local_1", "anno_2", true);
        assertReflectionEquals(annv3_2, an);

        an = ftService.getAnnotationV3(
                "dataset_1", "local_1", "anno_3", true);
        assertReflectionEquals(annv3_3, an);
    }

}
