package eu.europeana.fulltext.loader.test;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.entity.Target;
import eu.europeana.fulltext.loader.config.LoaderSettings;
import eu.europeana.fulltext.loader.exception.LoaderException;
import eu.europeana.fulltext.loader.service.ProgressLogger;
import eu.europeana.fulltext.loader.service.XMLParserService;
import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Tests the XMLXPathParser using 2 example data xml files
 * @author Patrick Ehlert
 * Created on 20-08-2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = "classpath:loader-test.properties")
@SpringBootTest(classes = {LoaderSettings.class})
public class XMLParserServiceTest {

    private static AnnoPage annoPage1;
    private static AnnoPage annoPage2;

    @Autowired
    private LoaderSettings settings;

    @Before
    public void loadExampleFiles() throws LoaderException, IOException {
        XMLParserService parser = new XMLParserService(settings);

        // This made-up example file is based on an existing one and contains an image entity with special characters
        // (e.g. &apos;). There are 78 annotations and one of the annotations (d10b792f3170d6b9f1628729c08fa293) has 2
        // targets and another one (1b51b0714445f9d442f4e230ba712ac1) has a simplified annotation
        String file1 = "9200396-BibliographicResource_3000118435009-1.xml";
        annoPage1 = parser.parse("1", loadXmlFile(file1), file1);

        // this example file contains 1111 annotations, including 4 annotations without a target (of which 2 annotations
        // have the exact same id)
        String file2 = "9200357-BibliographicResource_3000095247417-2.xml";
        annoPage2 = parser.parse("2", loadXmlFile(file2), file2);
    }

    private InputStream loadXmlFile(String fileName) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
        if (is != null) {
            return is;
        }
        throw new FileNotFoundException(fileName);
    }

    /**
     * Test if the various AnnotationPage ids (datasetId, localId, pageId and targetId) are retrieved and saved properly
     * from an example xml file
     */
    @Test
    public void testAnnotationPageIds() {
        assertEquals("9200396", annoPage1.getDsId());
        assertEquals("BibliographicResource_3000118435009", annoPage1.getLcId());
        assertEquals("1", annoPage1.getPgId());
        assertEquals("https://iiif.europeana.eu/image/7PFJIT3P3MO3RSA24XZ64IVYNMSV7MGXXR3MMBFNL7FLYHTKVHKQ/presentation_images/d0127a20-02ca-11e6-a651-fa163e2dd531/node-3/image/BNL/La_clef_du_cabinet_des_princes_de_l'Europe/1724/09/01/00161/full/full/0/default.jpg",
            annoPage1.getTgtId());

        assertEquals("9200357", annoPage2.getDsId());
        assertEquals("BibliographicResource_3000095247417", annoPage2.getLcId());
        assertEquals("2", annoPage2.getPgId());
        assertEquals("https://iiif.europeana.eu/image/5IA65CDOF34YRLIMTNOKBCOKRE55MMCUPIBRPZVBNCREWXJ6STHA/presentation_images/2dea77e0-0232-11e6-a696-fa163e2dd531/node-1/image/NLP/Głos_Śląski/1916/06/22/dodatek/00642/full/full/0/default.jpg",
            annoPage2.getTgtId());
    }

    /**
     * Test if all Resource ids (datasetId, localId and resourceId) are retrieved and saved properly.
     */
    @Test
    public void testResourceIds() {
        Resource res1 = annoPage1.getRes();
        assertEquals("9200396", res1.getDsId());
        assertEquals("BibliographicResource_3000118435009", res1.getLcId());
        assertEquals("575eecd7bc65dabe3ca7881001a22e03", res1.getId());

        Resource res2 = annoPage2.getRes();
        assertEquals("9200357", res2.getDsId());
        assertEquals("BibliographicResource_3000095247417", res2.getLcId());
        assertEquals("3136946e43beab1a0b2396ab18b63455", res2.getId());
    }

    /**
     * Test if the full text itself is retrieved properly from an example xml file
     */
    @Test
    public void testResourceText() {
        // no xml newline character should be present
        assertFalse(annoPage1.getRes().getValue().contains("&#xA"));
        assertFalse(annoPage2.getRes().getValue().contains("&#xA"));

        // we only check the start of this document because the rest is poorly OCR-ed and not entirely correct
        assertTrue(annoPage1.getRes().getValue().startsWith("LA CLEF DU CABINET\n"));
        assertTrue(annoPage2.getRes().getValue().startsWith("ići O\n"));
    }

    /**
     * Test if the language is retrieved properly from an example xml file
     */
    @Test
    public void testResourceLanguage() {
        assertEquals("fr", annoPage1.getRes().getLang());
        assertEquals("pl", annoPage2.getRes().getLang());
    }

    /**
     * Test if we have the correct number of annotations and if they (the first) is filled properly
     */
    @Test
    public void testAnnotations() {
        // there should be 78 annotation, including the page annotation
        assertEquals(78, annoPage1.getAns().size());
        // 1111 annotations minus 4 that have no proper target (so those are skipped)
        assertEquals(1111-4, annoPage2.getAns().size());

        // we also check the values of the first processed annotation in the first test file
        Annotation annotation1 = annoPage1.getAns().get(0);
        assertEquals("64764db9df0b1e87843b2fa3fec0a384", annotation1.getAnId());
        //assertEquals("http://www.w3.org/ns/oa#transcribing", annotation1.getMotiv());
        assertEquals('W', annotation1.getDcType());
        assertEquals(1, annotation1.getTgs().size());
        testTarget(annotation1.getTgs().get(0), 313, 239, 174, 74);
        testCharFromTo(annotation1, 0, 2);
        assertNull(annotation1.getLang());

        // check proper values of first annotation in second test file
        Annotation annotation2 = annoPage2.getAns().get(0);
        assertEquals("61be9cf0c12760ddce9de59b77e8a490", annotation2.getAnId());
        //assertEquals("http://www.w3.org/ns/oa#transcribing", annotation2.getMotiv());
        assertEquals('W', annotation2.getDcType());
        assertEquals(1, annotation2.getTgs().size());
        testTarget(annotation2.getTgs().get(0), 1683,0,144,66);
        testCharFromTo(annotation2, 0, 3);
        assertEquals("en", annotation2.getLang());
    }

    /**
     * Tests if annotations with more than 1 target are processed okay
     */
    @Test
    public void testAnnotationWithDoubleTarget() {
        Annotation a = getAnnotationWithId(annoPage1.getAns(), "d10b792f3170d6b9f1628729c08fa293");
        assertNotNull("Annotation with double target not found!", a);
        assertEquals(2, a.getTgs().size());
        testTarget(a.getTgs().get(0), 793,1425,133,34);
        testTarget(a.getTgs().get(1), 377,1488,63,33);
    }

    /**
     * Most annotations have a hasBody tag that contains a SpecificResource tag. However there can also be hasBody's
     * without a SpecificResource. This test checks if we handle this correctly.
     */
    @Test
    public void testAnnotationWithSimpleHasBody() {
        Annotation a = getAnnotationWithId(annoPage1.getAns(), "1b51b0714445f9d442f4e230ba712ac1");
        assertNotNull("Annotation with simple hasBody tag not found!", a);
        testCharFromTo(a, 285,318);
    }

    private Annotation getAnnotationWithId(List<Annotation> annotations, String anId) {
        Annotation result = null;
        for (Annotation a : annotations) {
            if (anId.equals(a.getAnId())) {
                result = a;
                break;
            }
        }
        return result;
    }

    /**
     * Since many targets are empty in the provided xml files, we test especially for proper handling of this
     */
    private void testTarget(Target target, Integer expectedX, Integer expectedY, Integer expectedW, Integer expectedH) {
        assertEquals(expectedX, target.getX());
        assertEquals(expectedY, target.getY());
        assertEquals(expectedW, target.getW());
        assertEquals(expectedH, target.getH());
    }

    private void testCharFromTo(Annotation anno, Integer expectedFrom, Integer expectedTo) {
        assertEquals(expectedFrom, anno.getFrom());
        assertEquals(expectedTo, anno.getTo());
    }

    /**
     * Test can be used to manually check changes in parser performance.
     */
    //@Test
    public void testPerformance() throws LoaderException, IOException{
        int attempts = 1000;
        ProgressLogger pl = new ProgressLogger(attempts, 15);
        for (int i = 0; i < attempts; i++) {
            loadExampleFiles();
            pl.addItemOk();
        }
        LogManager.getLogger(XMLParserServiceTest.class).info(pl.getResults());
    }

}