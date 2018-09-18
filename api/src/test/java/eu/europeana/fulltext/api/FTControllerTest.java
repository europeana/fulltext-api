package eu.europeana.fulltext.api;


import eu.europeana.fulltext.api.entity.AnnoPage;
import eu.europeana.fulltext.api.entity.Annotation;
import eu.europeana.fulltext.api.entity.Resource;
import eu.europeana.fulltext.api.entity.Target;
import eu.europeana.fulltext.api.model.v2.AnnotationPageV2;
import eu.europeana.fulltext.api.repository.AnnoPageRepository;
import eu.europeana.fulltext.api.repository.ResourceRepository;
import eu.europeana.fulltext.api.service.FTService;
import org.junit.jupiter.api.Assertions;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.BDDMockito.*;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


/**
 * Test the application's controller
 * @author Lúthien
 * Created on 28-02-2018
 */

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class FTControllerTest {

//    @Autowired
//    private WebApplicationContext context;

//    public Annotation(String       anId,
//                      String       dcType,
//                      Integer      from,
//                      Integer      to,
//                      List<Target> tgs) {
//        this(anId, dcType, from, to);
//        this.tgs = tgs;
//    }
//
//    public Annotation(String       anId,
//                      String       dcType,
//                      Integer      from,
//                      Integer      to,
//                      List<Target> tgs,
//                      String       lang) {
//        this(anId, dcType, from, to, tgs);
//        this.lang = lang;
//    }


    private static final Resource   RES1 = new Resource("resource-01", "twentsch", "Er ligt tussen Regge en Dinkel een land", "dataset-01", "local-01");
    private static final Resource   RES2 = new Resource("resource-02", "twentsch", "Er ligt tussen Regge en Dinkel een land", "dataset-01", "local-01");

    private static final AnnoPage ANP1 = new AnnoPage("dataset-01", "local-01", "page-01", "target-01", RES1);
    private static final AnnoPage ANP2 = new AnnoPage("dataset-01", "local-02", "page-01", "target-02", RES2);

    private static final Target     TAR1 = new Target(30, 100, 20, 12);

    private static final Annotation ANN1 = new Annotation("annotation-01", "W", 0, 2);

    private static final AnnotationPageV2 annotationPageV2_01 = new AnnotationPageV2("AnnotationPageV2Id-01");


    @Mock
    private FTService ftService;

    @BeforeEach
    void init() {
        annotationPageV2_01.setResources(
        given(ftService.getAnnotationPageV2("dataset-01", "local-01", "page-01")).willReturn()
        userService = new DefaultUserService(
                userRepository, settingRepository, mailClient);

        when(settingRepository.getUserMinAge()).thenReturn(10);
        when(settingRepository.getUserNameMinLength()).thenReturn(4);
        when(userRepository.isUsernameAlreadyExists(any(String.class)))
                .thenReturn(false);
    }



    /**
     * Loads the entire webapplication as mock server
     */
    @BeforeEach
    public void setup() {
        res = new Resource("resource-01", "twentsch", "Er ligt tussen Regge en Dinkel een land", "dataset-01", "local-01");
        anp = new AnnoPage("dataset-01", "local-01", "page-01", "target-01", res);
    }

    @Test
    public void testIdentify() throws Exception {
        resourceRepository.save(res);
        annoPageRepository.save(anp);
        List<AnnoPage> apList = annoPageRepository.findAll();
        assertNotNull(apList);
    }


}
