package eu.europeana.fulltext.loader.test;

import eu.europeana.fulltext.loader.config.LoaderSettings;
import eu.europeana.fulltext.loader.exception.LoaderException;
import eu.europeana.fulltext.loader.model.AnnoPageRdf;
import eu.europeana.fulltext.loader.model.AnnotationRdf;
import eu.europeana.fulltext.loader.model.TargetRdf;
import eu.europeana.fulltext.loader.service.XMLParserService;
import org.apache.commons.io.IOUtils;
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

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests the XMLXPathParser using 2 example data xml files
 * @author Patrick Ehlert
 * Created on 20-08-2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = "classpath:loader-test.properties")
@SpringBootTest(classes = {LoaderSettings.class})
public class XMLParserServiceTest {


    private static AnnoPageRdf apRdf1;
    private static AnnoPageRdf apRdf2;

    @Autowired
    private LoaderSettings settings;

    @Before
    public void loadExampleFiles() throws LoaderException, IOException {
        XMLParserService parser = new XMLParserService(settings);

        // this example file contains an image entity with special characters (e.g. &apos;), plus 78 annotations
        String file1 = "9200396-BibliographicResource_3000118435009-1.xml";
        apRdf1 = parser.eatIt(file1, loadXmlFile(file1), "1");

        // this example file contains 1111 annotations, including 4 annotations without a target of which 2 annotations
        // have the exact same id
        String file2 = "9200357-BibliographicResource_3000095247417-2.xml";
        apRdf2 = parser.eatIt(file2, loadXmlFile(file2), "2");
    }

    private String loadXmlFile(String fileName) throws IOException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            if (is != null) {
                return IOUtils.toString(is);
            }
            throw new FileNotFoundException(fileName);
        }
    }

    /**
     * Test if the various ids (datasetId, localId, resourceId and pageId) are retrieved properly from an example xml file
     */
    @Test
    public void testIds() {
        assertEquals("9200396", apRdf1.getDatasetId());
        assertEquals("BibliographicResource_3000118435009", apRdf1.getLocalId());
        assertEquals("575eecd7bc65dabe3ca7881001a22e03", apRdf1.getResourceId());
        assertEquals("1", apRdf1.getPageId());

        assertEquals("9200357", apRdf2.getDatasetId());
        assertEquals("BibliographicResource_3000095247417", apRdf2.getLocalId());
        assertEquals("3136946e43beab1a0b2396ab18b63455", apRdf2.getResourceId());
        assertEquals("2", apRdf2.getPageId());
    }

    /**
     * Test if the full text itself is retrieved properly from an example xml file
     */
    @Test
    public void testText() {
        // no xml newline character should be present
        assertFalse(apRdf1.getFtText().contains("&#xA"));
        assertFalse(apRdf2.getFtText().contains("&#xA"));

        // we only check the start of this document because the rest is poorly OCR-ed and not entirely correct
        assertTrue(apRdf1.getFtText().startsWith("LA CLEF DU CABINET\n"));
        assertTrue(apRdf2.getFtText().startsWith("ići O\n"));
    }

    /**
     * Test if the language is retrieved properly from an example xml file
     */
    @Test
    public void testLanguage() {
        assertEquals("fr", apRdf1.getFtLang());
        assertEquals("pl", apRdf2.getFtLang());
    }

    /**
     * Test if the image url is retrieved properly from an example xml file
     */
    @Test
    public void testImageUrl() {
        assertEquals("https://iiif.europeana.eu/image/7PFJIT3P3MO3RSA24XZ64IVYNMSV7MGXXR3MMBFNL7FLYHTKVHKQ/presentation_images/d0127a20-02ca-11e6-a651-fa163e2dd531/node-3/image/BNL/La_clef_du_cabinet_des_princes_de_l'Europe/1724/09/01/00161/full/full/0/default.jpg",
                     apRdf1.getImgTargetBase());

        assertEquals("https://iiif.europeana.eu/image/5IA65CDOF34YRLIMTNOKBCOKRE55MMCUPIBRPZVBNCREWXJ6STHA/presentation_images/2dea77e0-0232-11e6-a696-fa163e2dd531/node-1/image/NLP/Głos_Śląski/1916/06/22/dodatek/00642/full/full/0/default.jpg",
                     apRdf2.getImgTargetBase());
    }

    /**
     * Test if we have the correct number of annotations and if they (the first) is filled properly
     */
    @Test
    public void testAnnotations() {
        // there should be 1 page annotation which is kep separate, so 78-1
        assertEquals(78-1, apRdf1.getAnnotationRdfList().size());
        // 1111 annotations minus 1 page annotation minus 4 that have no proper target, so they are skipped
        assertEquals(1111-1-4, apRdf2.getAnnotationRdfList().size());

        // we also check the first processed annotation, first test file
        AnnotationRdf annotation1 = apRdf1.getAnnotationRdfList().get(0);
        assertEquals("64764db9df0b1e87843b2fa3fec0a384", annotation1.getId());
        assertEquals("http://www.w3.org/ns/oa#transcribing", annotation1.getMotiv());
        assertEquals("Word", annotation1.getDcType());
        assertEquals(1, annotation1.getTargetRdfList().size());
        testTarget(annotation1.getTargetRdfList().get(0), 313, 239, 174, 74);
        testCharFromTo(annotation1, 0, 2);
        assertNull(annotation1.getLang());

        // first annotation in second test file
        AnnotationRdf annotation2 = apRdf2.getAnnotationRdfList().get(0);
        assertEquals("61be9cf0c12760ddce9de59b77e8a490", annotation2.getId());
        assertEquals("http://www.w3.org/ns/oa#transcribing", annotation2.getMotiv());
        assertEquals("Word", annotation2.getDcType());
        assertEquals(1, annotation2.getTargetRdfList().size());
        testTarget(annotation2.getTargetRdfList().get(0), 1683,0,144,66);
        testCharFromTo(annotation2, 0, 3);
        assertNull(annotation2.getLang());
    }

    /**
     * Since many targets are empty in the provided xml files, we test especially for proper handling of this
     */
    public void testTarget(TargetRdf target, Integer expectedX, Integer expectedY, Integer expectedW, Integer expectedH) {
        assertEquals(expectedX, target.getX());
        assertEquals(expectedY, target.getY());
        assertEquals(expectedW, target.getW());
        assertEquals(expectedH, target.getH());
    }

    public void testCharFromTo(AnnotationRdf anno, Integer expectedFrom, Integer expectedTo) {
        assertEquals(expectedFrom, anno.getFrom());
        assertEquals(expectedTo, anno.getTo());

    }

    /**
     * Test if the page annotation is created properly
     */
    @Test
    public void testPageAnnotation() {
        AnnotationRdf pageAnnotation1 = apRdf1.getPageAnnotationRdf();
        assertEquals("d89c94e39f4a360da26a3fae58ff819a", pageAnnotation1.getId());
        assertEquals("http://www.w3.org/ns/oa#transcribing", pageAnnotation1.getMotiv());
        assertEquals("Page", pageAnnotation1.getDcType());
        assertTrue(pageAnnotation1.getTargetRdfList().isEmpty());
        assertNull(pageAnnotation1.getFrom());
        assertNull(pageAnnotation1.getTo());
        assertNull(pageAnnotation1.getLang());

        AnnotationRdf pageAnnotation2 = apRdf2.getPageAnnotationRdf();
        assertEquals("c00abd627c30be3f9332fc0e982f0574", pageAnnotation2.getId());
        assertEquals("http://www.w3.org/ns/oa#transcribing", pageAnnotation2.getMotiv());
        assertEquals("Page", pageAnnotation2.getDcType());
        assertTrue(pageAnnotation2.getTargetRdfList().isEmpty());
        assertNull(pageAnnotation2.getFrom());
        assertNull(pageAnnotation2.getTo());
        assertNull(pageAnnotation2.getLang());
    }


}