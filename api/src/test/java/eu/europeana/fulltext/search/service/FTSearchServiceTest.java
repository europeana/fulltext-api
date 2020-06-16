package eu.europeana.fulltext.search.service;

import eu.europeana.fulltext.api.config.FTDefinitions;
import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.service.EDM2IIIFMapping;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.search.model.response.Hit;
import eu.europeana.fulltext.search.model.response.HitSelector;
import eu.europeana.fulltext.search.model.response.SearchResult;
import eu.europeana.fulltext.search.repository.SolrNewspaperRepo;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit test for the FTSearchService class
 *
 * @author Patrick Ehlert
 * Created on 10 Jun 2020
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = "classpath:fulltext-test.properties")
@SpringBootTest(classes = {FTSearchService.class, FTSettings.class, EDM2IIIFMapping.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FTSearchServiceTest {

    // from page3 (start of page)
    private static final String SNIPPET_START_PAGE = "<em>Aus der</em> 49. Verlustliste.";
    private static final HitSelector HIT_START_PAGE = new HitSelector(null, "Aus der", " "); // 1 hit
    private static final String ANNO_START_PAGE_ID = "AnnoStartPage";
    private static final Annotation ANNO_START_PAGE = new Annotation(ANNO_START_PAGE_ID, FTDefinitions.ANNO_TYPE_WORD, 0, 7);

    // from page3 (end of page)
    private static final String SNIPPET_END_PAGE = "am 29. Oktober in der Philharmonie ein <em>zweites Konzert</em>";
    private static final HitSelector HIT_END_PAGE = new HitSelector(" ", "zweites Konzert", null); // 1 hits
    private static final String ANNO_END_PAGE_ID = "AnnoEndPage";
    private static final Annotation ANNO_END_PAGE = new Annotation(ANNO_END_PAGE_ID, FTDefinitions.ANNO_TYPE_WORD, 22970, 22985);

    // from page1 (middle of page)
    private static final String SNIPPET_2_DISTINCT_HITS = "Truck und Verlag: Rudolf Moffe in <em>Berlin</em>.\n<em>Berliner</em> Jageblalt 43.\\\"z.»rg.";
    private static final HitSelector HIT1_DISTINCT_HITS = new HitSelector(" ", "Berlin", ".");
    private static final HitSelector HIT2_DISTINCT_HITS = new HitSelector("\n", "Berliner", " ");

    // from page3 (middle of page)
    private static final String SNIPPET_2_SAME_HITS = "Paul E h r e n f e l d (<em>Berlin</em>) tot. Pion. Fritz Hage n (<em>Berlin</em>) tot.";
    private static final HitSelector HIT_2_SAME_HITS = new HitSelector("(", "Berlin", ")"); // 65 hits

    // from page3 (middle of page, before snippet is a space)
    private static final HitSelector HIT_NO_PREFIX_SPACE = new HitSelector("", "Erich", " "); // 2 hits

    // from page 3 (middle of page, before snippet is a newline
    private static final HitSelector HIT_NO_PREFIX_NEWLINE = new HitSelector (null,"Kommandowechsel", " "); // 1 hit
    private static final String ANNO_NO_PREFIX_NEWLINE_ID = "AnnoNoPrefix";
    private static final Annotation ANNO_NO_PREFIX_NEWLINE = new Annotation(ANNO_NO_PREFIX_NEWLINE_ID, FTDefinitions.ANNO_TYPE_WORD, 10764, 10779);

    // from page3 (middle of page, after snippet is a space)
    private static final HitSelector HIT_NO_SUFFIX_SPACE = new HitSelector (" ","Kirkwall", ""); // 1 hit
    private static final String ANNO1_NO_SUFFIX_SPACE_ID = "AnnoNoSuffix1";
    private static final Annotation ANNO1_NO_SUFFIX_SPACE = new Annotation(ANNO1_NO_SUFFIX_SPACE_ID, FTDefinitions.ANNO_TYPE_WORD, 12270, 12280);
    private static final String ANNO2_NO_SUFFIX_SPACE_ID = "AnnoNoSuffix2";
    private static final Annotation ANNO2_NO_SUFFIX_SPACE = new Annotation(ANNO2_NO_SUFFIX_SPACE_ID, FTDefinitions.ANNO_TYPE_WORD, 12272, 12278);


    // from page 3 (middle of page, after snippet is a newline)
    private static final HitSelector HIT_NO_SUFFIX_NEWLINE = new HitSelector(" ", "Raincourt", ""); // 1 hit
    private static final HitSelector HIT_NO_PREFIX_SUFFIX = new HitSelector("", "Raincourt", ""); // 1 hit

    @Autowired
    private FTSearchService searchService;

    @MockBean
    private SolrNewspaperRepo solrRepo;
    @MockBean
    private FTService fulltextRepo;

    private AnnoPage annoPage;

    @Before
    public void loadAnnoPage() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        String fulltext = IOUtils.toString(classloader.getResourceAsStream(
                "fulltext_9200355_BibliographicResource_3000096341989_page3.txt"), StandardCharsets.UTF_8);

        AnnoPage ap = new AnnoPage();
        Resource res = new Resource();
        res.setValue(fulltext);
        res.setId("test-resource3");
        ap.setPgId("3");
        ap.setRes(res);
        this.annoPage = ap;

        List<Annotation> annotations = new ArrayList<>() {
            {
                add(ANNO_START_PAGE);
                add(ANNO1_NO_SUFFIX_SPACE);
                add(ANNO2_NO_SUFFIX_SPACE);
                add(ANNO_NO_PREFIX_NEWLINE);
                add(ANNO_END_PAGE);
            }
        };
        ap.setAns(annotations);
    }

    /**
     * Test if extracting hits from a Solr Snippet works fine
     */
    @Test
    public void testAddHitsFromSolrSnippet() {
        Map<String, HitSelector> hits = new HashMap<>();
        searchService.getHitsFromSolrSnippet(hits, SNIPPET_START_PAGE, null);
        assertEquals(1, hits.keySet().size());
        HitSelector hit1 = hits.get( HIT_START_PAGE.toString());
        testHitSelector(hit1, HIT_START_PAGE);

        searchService.getHitsFromSolrSnippet(hits, SNIPPET_END_PAGE, null);
        assertEquals(2, hits.keySet().size());
        HitSelector hit2 = hits.get(HIT_END_PAGE.toString());
        testHitSelector(hit2, HIT_END_PAGE);

        searchService.getHitsFromSolrSnippet(hits, SNIPPET_2_DISTINCT_HITS, null);
        assertEquals(4, hits.keySet().size());
        HitSelector hit3 = hits.get(HIT1_DISTINCT_HITS.toString());
        testHitSelector(hit3, HIT1_DISTINCT_HITS);
        HitSelector hit4 = hits.get(HIT2_DISTINCT_HITS.toString());
        testHitSelector(hit4, HIT2_DISTINCT_HITS);

        searchService.getHitsFromSolrSnippet(hits, SNIPPET_2_SAME_HITS, null);
        assertEquals(5, hits.keySet().size());
        HitSelector hit5 = hits.get(HIT_2_SAME_HITS.toString());
        testHitSelector(hit5, HIT_2_SAME_HITS);
    }

    private void testHitSelector(HitSelector hit, HitSelector expectedHit) {
        assertNotNull(hit);
        assertNotNull(expectedHit);
        assertEquals(hit.toString(), expectedHit.getPrefix(), hit.getPrefix());
        assertEquals(expectedHit.getExact(), hit.getExact());
        assertEquals(expectedHit.getSuffix(), hit.getSuffix());
    }

     /**
      * Test if searching for a particular hit in a fulltext works fine.
     */
    @Test
    public void testFindHitFullText() {
        List<Hit> hits1a = searchService.findHitInFullText(HIT_2_SAME_HITS, annoPage, 100);
        assertEquals(65, hits1a.size());

        // test maxHits parameter
        List<Hit> hits1b = searchService.findHitInFullText(HIT_2_SAME_HITS, annoPage, 5);
        assertEquals(5, hits1b.size());

        // no prefix, before is a space
        List<Hit> hits2 = searchService.findHitInFullText(HIT_NO_PREFIX_SPACE, annoPage, 10);
        assertEquals(2, hits2.size());

        // no prefix, before is a newline
        List<Hit> hits3 = searchService.findHitInFullText(HIT_NO_PREFIX_NEWLINE, annoPage, 10);
        assertEquals(1, hits3.size());

        // no suffix, after is a space
        List<Hit> hit4 = searchService.findHitInFullText(HIT_NO_SUFFIX_SPACE, annoPage, 10);
        assertEquals(1, hit4.size());

        // no suffix, after is a newline
        List<Hit> hits5 = searchService.findHitInFullText(HIT_NO_SUFFIX_NEWLINE, annoPage, 10);
        assertEquals(1, hits5.size());

        // no prefix and suffix (not sure if Solr will ever return something like this, but to be sure)
        List<Hit> hits6 = searchService.findHitInFullText(HIT_NO_PREFIX_SUFFIX, annoPage, 10);
        assertEquals(1, hits5.size());
    }

    /**
     * Test if we handle not finding a hit properly
     */
    @Test
    public void testFindNoHit() {
        // although the test fulltext does contain a word with an x, there is no ' x '
        List<Hit> hits = searchService.findHitInFullText(new HitSelector(null, "x", null), annoPage, 10);
        assertEquals(0, hits.size());
    }

    /**
     * Test if we set the proper start and end coordinates
     */
    @Test
    public void testFindHitFulltextCoordinates() {
        String text = "This is another test test";
        AnnoPage annoPage = new AnnoPage("x", "y", "1", null,
                new Resource(null, null, text, null));

        HitSelector hitToFind = new HitSelector(null, "This", " ");
        List<Hit> hits = searchService.findHitInFullText(hitToFind, annoPage, 5);
        assertEquals(1, hits.size());
        Hit hitFound = hits.get(0);
        assertEquals(Integer.valueOf(0), hitFound.getStartIndex());
        assertEquals(Integer.valueOf(4), hitFound.getEndIndex());

        hitToFind = new HitSelector(" ", "another", " ");
        hits = searchService.findHitInFullText(hitToFind, annoPage, 5);
        assertEquals(1, hits.size());
        hitFound = hits.get(0);
        assertEquals(Integer.valueOf(8), hitFound.getStartIndex());
        assertEquals(Integer.valueOf(15), hitFound.getEndIndex());

        hitToFind = new HitSelector(" ", "test", null);
        hits = searchService.findHitInFullText(hitToFind, annoPage, 5);
        assertEquals(2, hits.size());
        hitFound = hits.get(0);
        assertEquals(Integer.valueOf(16), hitFound.getStartIndex());
        assertEquals(Integer.valueOf(20), hitFound.getEndIndex());
        hitFound = hits.get(1);
        assertEquals(Integer.valueOf(21), hitFound.getStartIndex());
        assertEquals(Integer.valueOf(25), hitFound.getEndIndex());
    }

    /**
     * Test if we can find a hit that is at the start of a fulltext and if we can find the corresponding Annotation
     */
    @Test
    public void testFindHitFulltextStart()  {
        List<Hit> hits = searchService.findHitInFullText(HIT_START_PAGE, annoPage, 10);
        assertEquals(1, hits.size());
        Hit startPageHit = hits.get(0);
        assertEquals(Integer.valueOf(0), startPageHit.getStartIndex());
        assertEquals(Integer.valueOf(HIT_START_PAGE.getExact().length()), startPageHit.getEndIndex());

        // find annotation
        SearchResult result = new SearchResult("test", true);
        searchService.findAnnotation(result, startPageHit, annoPage);
        assertEquals(Integer.valueOf(1), Integer.valueOf(result.getHits().size()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(result.getItems().size()));
        assertTrue(result.getItems().get(0).getId().endsWith(ANNO_START_PAGE_ID));
    }

    /**
     * Test if we can find a hit that is at the end of a fulltext and if we can find the corresponding Annotation
     */
    @Test
    public void testFindHitFulltextEnd()  {
        List<Hit> hits = searchService.findHitInFullText(HIT_END_PAGE, annoPage, 10);
        assertEquals(1, hits.size());
        Hit endPageHit = hits.get(0);
        int ftLength = annoPage.getRes().getValue().length();
        assertEquals(Integer.valueOf(ftLength - HIT_END_PAGE.getExact().length()), endPageHit.getStartIndex());
        assertEquals(Integer.valueOf(ftLength), endPageHit.getEndIndex());

        // find annotation
        SearchResult result = new SearchResult("test", true);
        searchService.findAnnotation(result, endPageHit, annoPage);
        assertEquals(Integer.valueOf(1), Integer.valueOf(result.getHits().size()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(result.getItems().size()));
        assertTrue(result.getItems().get(0).getId().endsWith(ANNO_END_PAGE_ID));
    }

    /**
     * Test if we can match a hit to an annotation
     */
    @Test
    public void testFindAnnotation() {
        // find annotation with a perfect match (same start and end coordinate)
        List<Hit> hit1 = searchService.findHitInFullText(HIT_NO_PREFIX_NEWLINE, annoPage, 10);
        assertEquals(1, hit1.size());
        SearchResult result1 = new SearchResult("test", true);
        searchService.findAnnotation(result1, hit1.get(0), annoPage);
        assertEquals(Integer.valueOf(1), Integer.valueOf(result1.getHits().size()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(result1.getItems().size()));
        assertTrue(result1.getItems().get(0).getId().endsWith(ANNO_NO_PREFIX_NEWLINE_ID));

        // find 2 overlapping annotations; one where the start coordinate is -1 and end coordinate +1 and another
        // where the start coordinate is +1 and the end coordinate -1.
        // This should not happen in practice of course.
        List<Hit> hit2 = searchService.findHitInFullText(HIT_NO_SUFFIX_SPACE, annoPage, 10);
        assertEquals(1, hit2.size());
        SearchResult result2 = new SearchResult("test", true);
        searchService.findAnnotation(result2, hit2.get(0), annoPage);
        assertEquals(Integer.valueOf(2), Integer.valueOf(result2.getHits().size()));
        assertEquals(Integer.valueOf(2), Integer.valueOf(result2.getItems().size()));
        assertTrue(result2.getItems().get(0).getId().endsWith(ANNO1_NO_SUFFIX_SPACE_ID));
        assertTrue(result2.getItems().get(1).getId().endsWith(ANNO2_NO_SUFFIX_SPACE_ID));
    }

    /**
     * Test if we handle finding no annotation okay (we don't add the hit)
     */
    @Test
    public void testFindNoAnnotations() {
        Hit noHit = new Hit(100, 101, new HitSelector(null, "x", null));
        SearchResult result = new SearchResult("test", true);
        searchService.findAnnotation(result, noHit, annoPage);
        assertTrue(result.getItems().isEmpty());
        assertTrue(result.getHits().isEmpty());
    }

}
