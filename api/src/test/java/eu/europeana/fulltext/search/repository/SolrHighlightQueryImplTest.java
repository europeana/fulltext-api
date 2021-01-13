package eu.europeana.fulltext.search.repository;

import eu.europeana.fulltext.search.model.query.EuropeanaId;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SolrHighlightQueryImplTest {

    private SolrHighlightQueryImpl shlQuery = new SolrHighlightQueryImpl();

    /**
     * Test if the created Solr query contains at least the most important query parameters (query, highlight query and
     * extended and method parameter)
     */
    @Test
    void testCreateQuery() {
        EuropeanaId testId = new EuropeanaId("x", "y");
        String testQuery = "TEST";
        int maxSnippets = 3;
        SolrQuery sq = shlQuery.createQuery(testId, testQuery, maxSnippets);
        assertNotNull(sq);
        assertEquals("europeana_id:\\/x\\/y", sq.getQuery());
        assertEquals(testQuery, sq.get("hl.q"));
        assertEquals("true", sq.get("hl.extended"));
        assertEquals("unified", sq.get("hl.method"));
    }
}
