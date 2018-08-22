package eu.europeana.fulltext.loader.test;

import eu.europeana.fulltext.loader.model.AnnoPageRdf;
import eu.europeana.fulltext.loader.service.XMLXPathParser;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests the XMLXPathParser (using example data)
 * @author Patrick Ehlert
 * Created on 20-08-2018
 */
public class XmlXPathParserTest {

    private static AnnoPageRdf apRdf1;
    private static String annoPage1FileContents;

    @BeforeClass
    public static void loadExampleFiles() throws IOException {
        // this example file contains an image entity with special characters (e.g. &apos;)
        annoPage1FileContents = loadXmlFile("9200396-BibliographicResource_3000118435009-1.xml");
        apRdf1 = XMLXPathParser.eatIt("9200396-BibliographicResource_3000118435009-1.xml", annoPage1FileContents, "1");
    }

    private static String loadXmlFile(String fileName) throws IOException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            if (is != null) {
                return IOUtils.toString(is);
            }
            throw new FileNotFoundException(fileName);
        }
    }

    // TODO also test PageAnnotationRdf object and list of AnnotationRdf

    /**
     * Test if the pageId is saved properly
     */
    @Test
    public void testPageId() {
        String testPageId = "1243";
        AnnoPageRdf apRdf = XMLXPathParser.eatIt("9200396-BibliographicResource_3000118435009-1.xml",
                annoPage1FileContents, testPageId);
        assertEquals(testPageId, apRdf.getPageId());
    }

    /**
     * Test if the full text resource id is retrieved properly from an example xml file
     */
    @Test
    public void testResourceId() {
        assertEquals("http://data.europeana.eu/fulltext/9200396/BibliographicResource_3000118435009/575eecd7bc65dabe3ca7881001a22e03",
                apRdf1.getFtResource());
    }

    /**
     * Test if the full text itself is retrieved properly from an example xml file
     */
    @Test
    public void testText() {
        // no xml newline character should be present
        assertFalse(apRdf1.getFtText().contains("&#xA"));
        // we only check the start of this document because the rest is poorly OCR-ed and not entirely correct
        assertTrue(apRdf1.getFtText().startsWith("LA CLEF DU CABINET\n"));
    }

    /**
     * Test if the language is retrieved properly from an example xml file
     */
    @Test
    public void testLanguage() {
        assertEquals("fr", apRdf1.getFtLang());
    }

    /**
     * Test if the image url is retrieved properly from an example xml file
     */
    @Test
    public void testAnnoPageImageUrl() {
        assertEquals("https://iiif.europeana.eu/image/7PFJIT3P3MO3RSA24XZ64IVYNMSV7MGXXR3MMBFNL7FLYHTKVHKQ/presentation_images/d0127a20-02ca-11e6-a651-fa163e2dd531/node-3/image/BNL/La_clef_du_cabinet_des_princes_de_l'Europe/1724/09/01/00161/full/full/0/default.jpg",
                apRdf1.getImgTargetBase());
    }


}
