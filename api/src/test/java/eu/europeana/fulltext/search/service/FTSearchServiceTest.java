package eu.europeana.fulltext.search.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.morphia.query.internal.MorphiaCursor;
import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.model.v2.AnnotationV2;
import eu.europeana.fulltext.api.service.EDM2IIIFMapping;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.search.exception.RecordDoesNotExistException;
import eu.europeana.fulltext.search.model.query.EuropeanaId;
import eu.europeana.fulltext.search.model.response.Hit;
import eu.europeana.fulltext.search.model.response.SearchResult;
import eu.europeana.fulltext.search.model.response.v2.SearchResultV2;
import eu.europeana.fulltext.search.model.response.v3.SearchResultV3;
import eu.europeana.fulltext.search.repository.SolrRepo;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.solr.common.util.NamedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

/**
 * Unit test for the FTSearchService class
 *
 * @author Patrick Ehlert
 * Created on 10 Jun 2020
 */
@TestPropertySource(locations = "classpath:fulltext-test.properties")
@SpringBootTest(classes = {FTSearchService.class, FTSettings.class, EDM2IIIFMapping.class})
public class FTSearchServiceTest {

    // Create dummy Solr result (for query "presentation/9200396/BibliographicResource_3000118435970/search?query=flandre")
    // --------------------------------------------------------------------------------------------
    private static final NamedList HIT1 = new NamedList(){{
        add("startOffsetUtf16", 95053);
        add("matchStartsUtf16", new ArrayList<>(List.of(95987, 96399, 0, 0, 0, 0, 0, 0)));
        add("matchEndsUtf16", new ArrayList<>(List.of(95994, 96406, 0, 0, 0, 0, 0, 0)));
    }};
    private static final NamedList HIT2 = new NamedList(){{
        add("startOffsetUtf16", 96790);
        add("matchStartsUtf16", new ArrayList<>(List.of(97730, 0, 0, 0, 0, 0, 0, 0)));
        add("matchEndsUtf16", new ArrayList<>(List.of(97737, 0, 0, 0, 0, 0, 0, 0)));
    }};
    private static final String SNIPPET1 = "{https://iiif.europeana.eu/image/AVB5EMAWXYXPY2NSRANVZQ77OZBQWXFUFDDJ74OTHTVU6OBRAHIA/presentation_images/60de4440-022a-11e6-a696-fa163e2dd531/node-3/image/BNL/Journal_historique_et_littéraire/1774/07/15/00123/full/full/0/default.jpg} JUILLET. 1774, de la petite-vérole, même à celles qui font de fervice auprès de Ca Perfonne, de paroltre à la Cour pendant le danger de la contagion. Avant le départ de Sa Maj. de la Muette , le Peuple s’y étoit rendu plufieurs fois en foule , criant Vive le Roi & point d’inoculation , & il a été remis au Roi plufieurs mémoires anonymes fur des accidents  caufés par l'inoculation , mais rien n’a pd faire changer Sa Majefté de réfolution (*>\n" +
            "«5\n" +
            "PAYS-BAS.\n" +
            "B R u x elles (7e a3 Juin. ) Monfei-gneur l’Archiduc accompagné du Prince de Stahremberg, Miniftre Plénipotentiaire, du Comte de Lamberg , & de Mr. de Crum-pipen, eft parti lundi dernier pour Tournay, d’où ce Séréniflime Prince fe rendra en Flandre  pour voir les principales Villes de la Province, les coupures, l’Eclufe de Schli-ken & les ouvrages de Mer. Le Comte de Rofemberg n’a pu être du voïage à caufe d’une nouvelle attaque de goutte. Ostbnde ( le 24 Juin. ) Avanthier à huit heures du foir S. A. R. Mgr. l’Archiduc  Maximilien , accompagné de plufieurs perfonnes de diftinftion , arriva en cette Ville, venant de Tournay & autres Villes de la Flandre autrichienne, & defcendit à l’Hôtel-de-Ville, où on avoit préparé un logement pour lui & pour fa fuite. Le len-\n" +
            "(*) Des raifons particulières nous aïant engagés  à finir la fécondé partie du Journal avant le 15, nous ne pouvons rafsùrer encore le Public fur les fuites de l’inoculation du Roi & de la ■famille roïale , qui julqu’ici ne préfente rieti qui puiflè fonder des craintes.";
    private static final String SNIPPET2 = "{https://iiif.europeana.eu/image/AVB5EMAWXYXPY2NSRANVZQ77OZBQWXFUFDDJ74OTHTVU6OBRAHIA/presentation_images/60de4440-022a-11e6-a696-fa163e2dd531/node-3/image/BNL/Journal_historique_et_littéraire/1774/07/15/00124/full/full/0/default.jpg} J V I LL if. îfjii, demain mâtin de Prince alla voir la Ville', les Fortifications & les autres ouvrages remarquables  ,■ & revint dîner à l’Hôtel-de -Tille. Après-dîner S. A.- R. fe rendit aux Bancs des huîtres, de là par le petit trajet du port aux Eclufes de Slykens, & enfuite aux Moulins à ■ feièr, d’ôù Elle partit pour Bru- -ges avec fa compagnie dans une barque nouvellement cônftruiie pour le paffage de Bruges à Gand. ■ G an b (/e 47 juin.') Mgr.- l’Archiduc Maximilien après avoir vu ,à Bruges ce qu’il y a de remarquablè, en partit le 25 dans la barque neuve , & arriva ici à cinq heures après midi , accompagné des Seigneurs quî ont fait le voyage avec lui & de quelques Députés des Etats de Flandre. S. A. R. allâ voir d’abord la nouvelle maifon de force, de là Elle alla faire un tour dans les principales  rues de la T ille , en carroflè à fix chevaux,  avec S. A. le Prince de Stahrernbèrg le Comte de Lâmberg & Mr. notre Evêque,  & alla defeendre à l’Abbaye de faint Pierre où elle logea. Ce Prince y foupï avec fa fuite & plufieurs autres perfonnes de diitinftion , & hier il entendit la MeiT® q l’Eglife Cathédrale de St. Bavon , d’où il fut dîner chez Mr. l’Evêqué. L’après-midt il alla voir tirer l’oie par ceux du Serment de St. Sebaftien, & où tous les autres Sermons  avoient été invités. Le foir il y eut! grand fouper à l’Hôtel-dc-Vilîé, fuivi d’un beau Bal paré ; & ce matin S. A. R. ave® fa'fuite a repris le chemin de Bruxelles, • i\n" +
            "fù'4";
    private static final NamedList SOLR_RESULT = new NamedList(){{
        add("snippets", new ArrayList(List.of(SNIPPET1, SNIPPET2)));
        add("passages", new ArrayList<>(List.of(HIT1, HIT2)));
    }};
    private static final Map<String, List<String>> SOLR_RESPONSE = new HashMap(){{
        // Note that the Map actually contains 1 NamedList value, but we say it's a List<String> because that matches
        // what the Solr QueryReponse.getHighlighting() method is returning.
        put("fulltext.fr", SOLR_RESULT);
    }};

    private static final Map<String, List<String>> SOLR_EMPTY_RESPONSE = new HashMap<>();

    private static final EuropeanaId RECORDID_HAS_RESULTS = new EuropeanaId("9200396", "BibliographicResource_3000118435970");
    private static final String QUERY_HAS_RESULTS = "flandre";
    private static final EuropeanaId RECORDID_NOT_EXISTS = new EuropeanaId("not", "exists");


    @Autowired
    private FTSearchService searchService;

    @MockBean
    private SolrRepo solrRepo;
    @MockBean
    private FTService fulltextRepo;
    @MockBean
    private MorphiaCursor<AnnoPage> morphiaCursor;

    @BeforeEach
    public void setupMocks() throws EuropeanaApiException {
        // default we return empty Solr results
        given(solrRepo.getHighlightsWithOffsets(any(EuropeanaId.class), anyString(), anyInt(), anyObject())).willReturn(
                SOLR_EMPTY_RESPONSE
        );
        // but we do return results when recordId = /x/y and query = flandre
        given(solrRepo.getHighlightsWithOffsets(eq(RECORDID_HAS_RESULTS), eq(QUERY_HAS_RESULTS), anyInt(), anyObject())).willReturn(
                SOLR_RESPONSE
        );

        // default no AnnoPages are available for any record
        given(fulltextRepo.getAnnoPages(any(), any(), any(), anyBoolean())).willReturn(
                Collections.emptyList()
        );
        // except for 1 record
        given(fulltextRepo.getAnnoPages(eq(RECORDID_HAS_RESULTS.getDatasetId()), eq(RECORDID_HAS_RESULTS.getLocalId()),
                any(), anyBoolean())).willReturn(
                        Arrays.asList(new AnnoPage())
        );
    }

    /**
     * Load 2 AnnoPages from file
     * @return List of loaded AnnoPages containing annotations of all types
     */
    private List<AnnoPage> loadAnnoPages() {
        ObjectMapper o = new ObjectMapper();
        o.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        List<AnnoPage> result = new ArrayList<>();
        // load the json files of 2 annopages
        try (InputStream s1 = classloader.getResourceAsStream("9200396_BibliographicResource_3000118435970_annopage_61.json");
             InputStream s2 = classloader.getResourceAsStream("9200396_BibliographicResource_3000118435970_annopage_62.json")) {
            result.add(o.readValue(s1, AnnoPage.class));
            result.add(o.readValue(s2, AnnoPage.class));
        } catch (IOException e) {
            LogManager.getLogger(FTSearchServiceTest.class).error("Error reading stored annotation pages", e);
        }
        assertEquals(2, result.size());
        return result;
    }

    /**
     * Since the test will initially load 2 annopages with all annotation types, this method allow us to filter
     * based on specific types so we can better mock the morphia cursor returning only annotations of requested type(s)
     * @param annoTypes list of annotation types to filter the loaded AnnoPages
     * @return copy of the List of loaded annopages with only the annotations with the provided annotation types
     */
    private List<AnnoPage> filterByType(List<AnnotationType> annoTypes) {
        List<AnnoPage> result = loadAnnoPages();
        for (AnnoPage ap : result) {
            List<Annotation> filtered = ap.getAns().stream().
                    filter(ans -> annoTypes.contains(AnnotationType.fromAbbreviation(ans.getDcType()))).collect(Collectors.toList());
            ap.setAns(filtered);
        }
        return result;
    }

    /**
     * This will mock a morphia cursor returning 2 annopages with annotations of the requested type(s).
     * This method needs to be called before calling the SearchService.searchIssue() method
     */
    private void mockMorphiaCursor(List<AnnotationType> annoTypes){
        List<AnnoPage> filteredAnnoPages = filterByType(annoTypes);

        given(morphiaCursor.hasNext())
                // This is a bit of a hack, we know we will request hasNext() 1 extra time in the code so we should
                // return true 3 times so we can return 2 items and after that return false
                // However if our code changes and calls hasNext() more often or less often, this may need to be adjusted
                .willReturn(true)
                .willReturn(true)
                .willReturn(true)
                .willReturn(false);
        given(morphiaCursor.next())
                .willReturn(filteredAnnoPages.get(0))
                .willReturn(filteredAnnoPages.get(1));
        given(fulltextRepo.fetchAnnoPageFromTargetId(eq(RECORDID_HAS_RESULTS.getDatasetId()), eq(RECORDID_HAS_RESULTS.getLocalId()),
                any(), any(), anyBoolean())).willReturn(
                morphiaCursor
        );
    }

    /**
     * This tests if we can retrieve a v2 search result with only line annotations
     * Test also checks if we have:
     * <pre>
     *  - the expected annotations (annotation ids)
     *  - if annotations have an image ("on" field)
     *  - the expected hits (annotation ids)
     *  - if each hit has the expected prefix, exact and suffix
     * </pre>
     */
    @Test
    void testRetrieveResultsV2Line() throws EuropeanaApiException {
        List<AnnotationType> annoTypes = List.of(AnnotationType.LINE);
        mockMorphiaCursor(annoTypes);
        String searchId = "testV2";

        SearchResult result = searchService.searchIssue(searchId, RECORDID_HAS_RESULTS, QUERY_HAS_RESULTS, 12, annoTypes, "2", false);
        assertTrue(result instanceof SearchResultV2);
        SearchResultV2 resultV2 = (SearchResultV2) result;
        assertEquals(searchId, resultV2.getId());

        // Test annotations
        assertNotNull(resultV2.getItems());
        assertEquals(3, resultV2.getItems().size());

        String annotationRecordId = "/annotation" + RECORDID_HAS_RESULTS.toString();
        String id1 = "/bd498efe85327039cd1f48a9f4ccb8ec";
        AnnotationV2 item1 = resultV2.getItems().get(0);
        assertTrue(item1.getId().endsWith(annotationRecordId + id1), item1.getId());
        assertEquals(1, item1.getOn().length);
        assertTrue(StringUtils.isNoneEmpty(item1.getOn()[0]));

        String id2 = "/4a3b1091f1ab484af3eaeec52bd1b721";
        AnnotationV2 item2 = resultV2.getItems().get(1);
        assertTrue(item2.getId().endsWith(annotationRecordId + id2), item2.getId());
        assertEquals(1, item2.getOn().length);
        assertTrue(StringUtils.isNoneEmpty(item2.getOn()[0]));

        String id3 = "/2264ba0fc35330f14bcc3c3e0a8d4e96";
        AnnotationV2 item3 = resultV2.getItems().get(2);
        assertTrue(item3.getId().endsWith(annotationRecordId + id3), item3.getId());
        assertEquals(1, item3.getOn().length);
        assertTrue(StringUtils.isNoneEmpty(item3.getOn()[0]));

        // Test hits
        assertNotNull(resultV2.getHits());
        assertEquals(3, resultV2.getHits().size());

        Hit hit1 = resultV2.getHits().get(0);
        assertEquals(1, hit1.getAnnotations().size());
        assertTrue(hit1.getAnnotations().get(0).endsWith(annotationRecordId + id1), hit1.getAnnotations().get(0));
        assertEquals("d’où ce Séréniflime Prince fe rendra en ", hit1.getSelectors().get(0).getPrefix());
        assertEquals("Flandre", hit1.getSelectors().get(0).getExact());
        assertEquals("", hit1.getSelectors().get(0).getSuffix());

        Hit hit2 = resultV2.getHits().get(1);
        assertEquals(1, hit2.getAnnotations().size());
        assertTrue(hit2.getAnnotations().get(0).endsWith(annotationRecordId + id2), hit2.getAnnotations().get(0));
        assertEquals("de la ", hit2.getSelectors().get(0).getPrefix());
        assertEquals("Flandre", hit2.getSelectors().get(0).getExact());
        assertEquals(" autrichienne, & defcendit à", hit2.getSelectors().get(0).getSuffix());

        Hit hit3 = resultV2.getHits().get(2);
        assertEquals(1, hit3.getAnnotations().size());
        assertTrue(hit3.getAnnotations().get(0).endsWith(annotationRecordId + id3), hit3.getAnnotations().get(0));
        assertEquals("Députés des Etats de ", hit3.getSelectors().get(0).getPrefix());
        assertEquals("Flandre", hit3.getSelectors().get(0).getExact());
        assertEquals(". S. A. R. allâ", hit3.getSelectors().get(0).getSuffix());
    }

    /**
     * Test V3 search result with block line and word annotations
     * @throws EuropeanaApiException
     */
    @Test
    void testRetrieveResultsV3BlockLineWord() throws EuropeanaApiException {
        List<AnnotationType> annoTypes = List.of(AnnotationType.BLOCK, AnnotationType.LINE, AnnotationType.WORD);
        mockMorphiaCursor(annoTypes);
        String searchId = "testV3";

        SearchResult result = searchService.searchIssue(searchId, RECORDID_HAS_RESULTS, QUERY_HAS_RESULTS, 12, annoTypes, "3", false);
        assertTrue(result instanceof SearchResultV3);
        SearchResultV3 resultV3 = (SearchResultV3) result;
        assertEquals(searchId, resultV3.getId());

        // Test annotations
        assertNotNull(resultV3.getItems());
        assertEquals(9, resultV3.getItems().size());

        // Test hits
        assertNotNull(resultV3.getHits());
        assertEquals(6, resultV3.getHits().size());
    }

    /**
     * Test if the pageSize parameter works
     */
    @Test
    void testMaxPageSize() throws EuropeanaApiException {
        List<AnnotationType> annoTypes = List.of(AnnotationType.BLOCK, AnnotationType.LINE, AnnotationType.WORD);
        mockMorphiaCursor(annoTypes);
        int maxPageSize = 2;

        SearchResult result = searchService.searchIssue(null, RECORDID_HAS_RESULTS, QUERY_HAS_RESULTS, maxPageSize, annoTypes, "2", true);
        assertTrue(result instanceof SearchResultV2);
        SearchResultV2 resultV2 = (SearchResultV2) result;

        assertNotNull(resultV2.getItems());
        assertEquals(maxPageSize, resultV2.getItems().size());
    }

    /**
     * Test if we return a result with no hits if only Word-level annotations are asked
     */
    @Test
    void testRetrieveResultsNoHitsForWordAnnotations() throws EuropeanaApiException {
        List<AnnotationType> annoTypes = List.of(AnnotationType.WORD);
        mockMorphiaCursor(annoTypes);

        SearchResult result = searchService.searchIssue(null, RECORDID_HAS_RESULTS, QUERY_HAS_RESULTS, 12, annoTypes, "3", true);
        assertNotNull(result.getHits());
        assertEquals(0, result.getHits().size());
    }

    /**
     * Test if we handle not finding a solr hit properly (even though the record exists)
     */
    @Test
    void testNoSolrResultRecordExists() throws EuropeanaApiException {
        SearchResult resultV2 = searchService.searchIssue("test", RECORDID_HAS_RESULTS, "Some other query", 12, List.of(AnnotationType.BLOCK), "3", false);
        assertNotNull(resultV2);
        assertTrue(resultV2.getHits().isEmpty());
    }

    /**
     * Test if we handle not finding a solr hit properly (because record doesn't exist)
     */
    @Test
    void testNoSolrResultRecordNotExists() throws EuropeanaApiException {
        assertThrows(RecordDoesNotExistException.class, () -> {
            searchService.searchIssue("test", RECORDID_NOT_EXISTS, "Flandres", 12, List.of(AnnotationType.BLOCK), "3", false);
        });
    }

}
