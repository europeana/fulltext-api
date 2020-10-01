package eu.europeana.fulltext.search.service;

import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.service.EDM2IIIFMapping;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for the FTSearchService class
 *
 * @author Patrick Ehlert
 * Created on 10 Jun 2020
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = "classpath:fulltext-test.properties")
@SpringBootTest(classes = {FTSearchService.class, FTSettings.class, EDM2IIIFMapping.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FTSearchServiceTest {

//    private static final String searchVersion = "3";
//    // from page3 (start of page)
//    private static final String SNIPPET_START_PAGE = "<em>Aus der</em> 49. Verlustliste.";
//    private static final SolrSnippetOffset HIT_START_PAGE = new SolrSnippetOffset(null, "Aus der", ' ', -1, -1); // 1 hit
//    private static final String ANNO_START_PAGE_ID = "AnnoStartPage";
//    private static final Annotation ANNO_START_PAGE = new Annotation(ANNO_START_PAGE_ID, AnnotationType.LINE.getAbbreviation(),
//            0, 7);
//
//    // from page3 (end of page)
//    private static final String SNIPPET_END_PAGE = "am 29. Oktober in der Philharmonie ein <em>zweites Konzert</em>";
//    private static final SolrSnippetOffset HIT_END_PAGE = new SolrSnippetOffset(' ', "zweites Konzert", null, -1, -1); // 1 hits
//    private static final String ANNO_END_PAGE_ID = "AnnoEndPage";
//    private static final Annotation ANNO_END_PAGE = new Annotation(ANNO_END_PAGE_ID,  AnnotationType.WORD.getAbbreviation(),
//            22970, 22985);
//
//    // from page3 (middle of page)
//    private static final String SNIPPET_2_SAME_HITS = "Paul E h r e n f e l d (<em>Berlin</em>) tot. Pion. Fritz Hage n (<em>Berlin</em>) tot.";
//    private static final SolrSnippetOffset HIT_2_SAME_HITS = new SolrSnippetOffset('(', "Berlin", ')', -1, -1); // 65 hits
//
//    // from page3 (middle of page, before snippet is a space)
//    private static final SolrSnippetOffset HIT_NO_PREFIX_SPACE = new SolrSnippetOffset(null, "Erich", ' ', -1, -1); // 2 hits
//
//    // from page 3 (middle of page, before snippet is a newline
//    private static final SolrSnippetOffset HIT_NO_PREFIX_NEWLINE = new SolrSnippetOffset(null,"Kommandowechsel", ' ', -1, -1); // 1 hit
//    private static final String ANNO_NO_PREFIX_NEWLINE_WORD_ID = "AnnoNoPrefixWord";
//    private static final String ANNO_NO_PREFIX_NEWLINE_BLOCK_ID = "AnnoNoPrefixBlock";
//    private static final Annotation ANNO_NO_PREFIX_NEWLINE_WORD = new Annotation(ANNO_NO_PREFIX_NEWLINE_WORD_ID,  AnnotationType.WORD.getAbbreviation(),
//            10764, 10779);
//    private static final Annotation ANNO_NO_PREFIX_NEWLINE_BLOCK = new Annotation(ANNO_NO_PREFIX_NEWLINE_BLOCK_ID,  AnnotationType.BLOCK.getAbbreviation(),
//            10700, 10800);
//
//    // from page3 (middle of page, after snippet is a space)
//    private static final SolrSnippetOffset HIT_NO_SUFFIX_SPACE = new SolrSnippetOffset(' ',"Kirkwall", null, -1, -1); // 1 hit
//    private static final String ANNO1_NO_SUFFIX_SPACE_ID = "AnnoNoSuffix1";
//    private static final Annotation ANNO1_NO_SUFFIX_SPACE = new Annotation(ANNO1_NO_SUFFIX_SPACE_ID,  AnnotationType.LINE.getAbbreviation(),
//            12270, 12280);
//    private static final String ANNO2_NO_SUFFIX_SPACE_ID = "AnnoNoSuffix2";
//    private static final Annotation ANNO2_NO_SUFFIX_SPACE = new Annotation(ANNO2_NO_SUFFIX_SPACE_ID,  AnnotationType.LINE.getAbbreviation(),
//            12272, 12278);
//
//    // from page 3 (middle of page, after snippet is a newline)
//    private static final SolrSnippetOffset HIT_NO_SUFFIX_NEWLINE = new SolrSnippetOffset(' ', "Raincourt", null, -1, -1); // 1 hit
//    private static final SolrSnippetOffset HIT_NO_PREFIX_SUFFIX = new SolrSnippetOffset("", "Raincourt", "", -1, -1); // 1 hit
//
//
//    @Autowired
//    private FTSearchService searchService;
//
//    @MockBean
//    private SolrRepo solrRepo;
//    @MockBean
//    private FTService fulltextRepo;
//
//    private AnnoPage annoPage;
//
//    @Before
//    public void loadAnnoPage() throws IOException {
//        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
//        String fulltext = IOUtils.toString(classloader.getResourceAsStream(
//                "fulltext_9200355_BibliographicResource_3000096341989_page3.txt"), StandardCharsets.UTF_8);
//
//        AnnoPage ap = new AnnoPage();
//        Resource res = new Resource();
//        res.setValue(fulltext);
//        res.setId("test-resource3");
//        ap.setPgId("3");
//        ap.setRes(res);
//        this.annoPage = ap;
//
//        List<Annotation> annotations = new ArrayList<>() {
//            {
//                add(ANNO_START_PAGE);
//                add(ANNO1_NO_SUFFIX_SPACE);
//                add(ANNO2_NO_SUFFIX_SPACE);
//                add(ANNO_NO_PREFIX_NEWLINE_WORD);
//                add(ANNO_NO_PREFIX_NEWLINE_BLOCK);
//                add(ANNO_END_PAGE);
//            }
//        };
//        ap.setAns(annotations);
//    }
//
//    /**
//     * Test if extracting hits/keywords from a Solr Snippet works fine
//     */
//    @Test
//    public void testGetSolrHitsFromSnippet() {
//        // find a keyword at the start of a page
//        List<SolrSnippetOffset> hits = new ArrayList<>();
//        searchService.getHitsFromSolrSnippet(hits, SNIPPET_START_PAGE, null);
//        assertEquals(1, hits.size());
//        SolrSnippetOffset hit1 = hits.get(0);
//        assertEquals(HIT_START_PAGE, hit1);
//
//        // find a keyword at the end of a page
//        searchService.getHitsFromSolrSnippet(hits, SNIPPET_END_PAGE, null);
//        assertEquals(2, hits.size());
//        SolrSnippetOffset hit2 = hits.get(1);
//        assertEquals(HIT_END_PAGE, hit2);
//
//        // find 2 different keywords in 1 snippet
//        String twoDistinctHits = "Truck und Verlag: <em>Berlin</em>.      \n<em>Berliner</em> Jageblalt 43.\\\"z.»rg.";
//        SolrSnippetOffset expectedHit1 = new SolrSnippetOffset(' ',"Berlin", '.', -1, -1);
//        SolrSnippetOffset expectedHit2 = new SolrSnippetOffset('\n', "Berliner", ' ', -1, -1);
//        searchService.getHitsFromSolrSnippet(hits, twoDistinctHits, null);
//        assertEquals(4, hits.size());
//        SolrSnippetOffset hit3 = hits.get(hits.indexOf(expectedHit1));
//        assertEquals(expectedHit1, hit3);
//        SolrSnippetOffset hit4 = hits.get(hits.indexOf(expectedHit2));
//        assertEquals(expectedHit2, hit4);
//
//        // find 2 exactly the same keywords (so should count as 1)
//        searchService.getHitsFromSolrSnippet(hits, SNIPPET_2_SAME_HITS, null);
//        assertEquals(5, hits.size());
//        SolrSnippetOffset hit5 = hits.get(hits.indexOf(HIT_2_SAME_HITS));
//        assertEquals(HIT_2_SAME_HITS, hit5);
//    }
//
//    /**
//     * Test if merging 2 solr hits found next to each other works fine.
//     */
//    @Test
//    public void testMergeSolrHitsFromSnippet() {
//        String snippet = "Na ELSENEUR, Kaptein <em>Daniel</em> <em>Ehlert</em>, van Koningsbergen";
//        SolrSnippetOffset expectedSolrHit = new SolrSnippetOffset(" ", "Daniel Ehlert", ",", -1, -1);
//
//        List<SolrSnippetOffset> hits = new ArrayList<>();
//        searchService.getHitsFromSolrSnippet(hits, snippet, null);
//        assertEquals(1, hits.size());
//        assertEquals(expectedSolrHit, hits.get(0));
//    }
//
//    /**
//     * Test if searching for a particular solrhit in a fulltext works fine.
//     */
//    @Test
//    public void testFindSolrHitInFullText() {
//        List<Hit> hits1a = searchService.findHitInFullText(HIT_2_SAME_HITS, annoPage, 100, searchVersion);
//        assertEquals(65, hits1a.size());
//
//        // test maxHits parameter
//        List<Hit> hits1b = searchService.findHitInFullText(HIT_2_SAME_HITS, annoPage, 5, searchVersion);
//        assertEquals(5, hits1b.size());
//
//        // no prefix, before is a space
//        List<Hit> hits2 = searchService.findHitInFullText(HIT_NO_PREFIX_SPACE, annoPage, 10, searchVersion);
//        assertEquals(2, hits2.size());
//
//        // no prefix, before is a newline
//        List<Hit> hits3 = searchService.findHitInFullText(HIT_NO_PREFIX_NEWLINE, annoPage, 10, searchVersion);
//        assertEquals(1, hits3.size());
//
//        // no suffix, after is a space
//        List<Hit> hit4 = searchService.findHitInFullText(HIT_NO_SUFFIX_SPACE, annoPage, 10, searchVersion);
//        assertEquals(1, hit4.size());
//
//        // no suffix, after is a newline
//        List<Hit> hits5 = searchService.findHitInFullText(HIT_NO_SUFFIX_NEWLINE, annoPage, 10, searchVersion);
//        assertEquals(1, hits5.size());
//
//        // no prefix and suffix (not sure if Solr will ever return something like this, but to be sure)
//        List<Hit> hits6 = searchService.findHitInFullText(HIT_NO_PREFIX_SUFFIX, annoPage, 10, searchVersion);
//        assertEquals(1, hits5.size());
//    }
//
//    /**
//     * Test if we handle not finding a solrhit properly
//     */
//    @Test
//    public void testFindNoSolrHit() {
//        // although the test fulltext does contain a word with an x, there is no ' x '
//        List<Hit> hits = searchService.findHitInFullText(new SolrSnippetOffset("", "x", "", -1, -1), annoPage, 10, searchVersion);
//        assertEquals(0, hits.size());
//    }
//
//    /**
//     * Test if we set the proper start and end coordinates when we find a solrhit and create a hit object
//     */
//    @Test
//    public void testFindSolrHitInFulltextCoordinates() {
//        String text = "This is another test test";
//        AnnoPage annoPage = new AnnoPage("x", "y", "1", null,
//                new Resource(null, null, text, null));
//
//        SolrSnippetOffset hitToFind = new SolrSnippetOffset(null, "This", ' ', -1, -1);
//        List<Hit> hits = searchService.findHitInFullText(hitToFind, annoPage, 5, searchVersion);
//        assertEquals(1, hits.size());
//        Hit hitFound = hits.get(0);
//        assertEquals(Integer.valueOf(0), hitFound.getStartIndex());
//        assertEquals(Integer.valueOf(4), hitFound.getEndIndex());
//
//        hitToFind = new SolrSnippetOffset(' ', "another", ' ', -1, -1);
//        hits = searchService.findHitInFullText(hitToFind, annoPage, 5, searchVersion);
//        assertEquals(1, hits.size());
//        hitFound = hits.get(0);
//        assertEquals(Integer.valueOf(8), hitFound.getStartIndex());
//        assertEquals(Integer.valueOf(15), hitFound.getEndIndex());
//
//        hitToFind = new SolrSnippetOffset(' ', "test", null, -1, -1);
//        hits = searchService.findHitInFullText(hitToFind, annoPage, 5, searchVersion);
//        assertEquals(2, hits.size());
//        hitFound = hits.get(0);
//        assertEquals(Integer.valueOf(16), hitFound.getStartIndex());
//        assertEquals(Integer.valueOf(20), hitFound.getEndIndex());
//        hitFound = hits.get(1);
//        assertEquals(Integer.valueOf(21), hitFound.getStartIndex());
//        assertEquals(Integer.valueOf(25), hitFound.getEndIndex());
//    }
//
//    /**
//     * Test if we can find a solrhit that is at the start of a fulltext
//     */
//    @Test
//    public void testFindSolrHitInFulltextStart() {
//        List<Hit> hits = searchService.findHitInFullText(HIT_START_PAGE, annoPage, 10, searchVersion);
//        assertEquals(1, hits.size());
//        Hit startPageHit = hits.get(0);
//        assertEquals(Integer.valueOf(0), startPageHit.getStartIndex());
//        assertEquals(Integer.valueOf(HIT_START_PAGE.getExact().length()), startPageHit.getEndIndex());
//    }
//
//    /**
//     * Test if we can find a hit that is at the end of a fulltext
//     */
//    @Test
//    public void testFindSolrHitInFulltextEnd() {
//        List<Hit> hits = searchService.findHitInFullText(HIT_END_PAGE, annoPage, 10, searchVersion);
//        assertEquals(1, hits.size());
//        Hit endPageHit = hits.get(0);
//        int ftLength = annoPage.getRes().getValue().length();
//        assertEquals(Integer.valueOf(ftLength - HIT_END_PAGE.getExact().length()), endPageHit.getStartIndex());
//        assertEquals(Integer.valueOf(ftLength), endPageHit.getEndIndex());
//    }
//
//    /**
//     * Test if we can match a hit to an annotation
//     */
//    @Test
//    public void testFindAnnotation() {
//        // find annotation with a perfect match (same start and end coordinate)
//        List<Hit> hit1 = searchService.findHitInFullText(HIT_NO_PREFIX_NEWLINE, annoPage, 10, searchVersion);
//        assertEquals(1, hit1.size());
//
//        SearchResultV3 result1 = new SearchResultV3("test", true);
//        searchService.findAnnotation(result1, hit1.get(0), annoPage, AnnotationType.BLOCK);
//        assertEquals(Integer.valueOf(1), Integer.valueOf(result1.getHits().size()));
//        assertEquals(Integer.valueOf(1), Integer.valueOf(result1.getItems().size()));
//        assertTrue(result1.getItems().get(0).getId().endsWith(ANNO_NO_PREFIX_NEWLINE_BLOCK_ID));
//
//        // if we retry again for word-level annotations we should still find 1 annotation, but there should be no hit
//        // in the result because we don't output those for word-level annotations
//        SearchResultV3 result2 = new SearchResultV3("test", true);
//        searchService.findAnnotation(result2, hit1.get(0), annoPage, AnnotationType.WORD);
//        assertTrue(result2.getHits().isEmpty());
//        assertEquals(Integer.valueOf(1), Integer.valueOf(result2.getItems().size()));
//        assertTrue(result2.getItems().get(0).getId().endsWith(ANNO_NO_PREFIX_NEWLINE_WORD_ID));
//
//        // find 2 overlapping annotations; one where the start coordinate is -1 and end coordinate +1 and another
//        // where the start coordinate is +1 and the end coordinate -1.
//        // This should not happen in practice of course.
//        List<Hit> hit2 = searchService.findHitInFullText(HIT_NO_SUFFIX_SPACE, annoPage, 10, searchVersion);
//        assertEquals(1, hit2.size());
//        SearchResultV3 result3 = new SearchResultV3("test", true);
//        searchService.findAnnotation(result3, hit2.get(0), annoPage, AnnotationType.LINE);
//        assertEquals(Integer.valueOf(1), Integer.valueOf(result3.getHits().size())); // should have only 1 hit
//        assertEquals(Integer.valueOf(2), Integer.valueOf(result3.getItems().size()));
//        assertTrue(result3.getItems().get(0).getId().endsWith(ANNO1_NO_SUFFIX_SPACE_ID));
//        assertTrue(result3.getItems().get(1).getId().endsWith(ANNO2_NO_SUFFIX_SPACE_ID));
//    }
//
//    /**
//     * Test if we handle finding no annotation okay (we don't add the hit)
//     */
//    @Test
//    public void testFindNoAnnotations() {
//        Hit noHit =  HitFactory.createHit(100, 101, "x", searchVersion);
//        SearchResult result = SearchResultFactory.createSearchResult("test", true, searchVersion);
//        searchService.findAnnotation(result, noHit, annoPage, AnnotationType.LINE);
//        assertEquals(0, result.itemSize());
//        assertTrue(result.getHits().isEmpty());
//    }

}
