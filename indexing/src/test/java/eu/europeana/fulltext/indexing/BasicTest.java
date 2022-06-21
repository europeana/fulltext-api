package eu.europeana.fulltext.indexing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.util.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class BasicTest {

    @Autowired
    private IndexingAnnoPageRepository repository;

    @Autowired
    private MetadataCollection metadataCollection;

    @Autowired
    private FulltextCollection fulltextCollection;

    @Test
     void test_fulltext() throws IOException, SolrServerException {
        List<String> ids = new ArrayList<>();
        ids.add("/9200396/BibliographicResource_3000118435009");
        ids.add("/9200396/BibliographicResource_3000118436165");

        fulltextCollection.deleteDocuments(ids);
        assertFalse(fulltextCollection.exists(ids.get(0)));
        assertFalse(fulltextCollection.exists(ids.get(0)));
        fulltextCollection.setMetadata(ids,metadataCollection);
        assertTrue(fulltextCollection.checkMetadata(ids.get(0)));
        assertTrue(fulltextCollection.checkMetadata(ids.get(1)));
        fulltextCollection.setFulltext(ids);
        assertEquals(LocalDateTime.of(2018, Month.JULY,11,14,54,57,295),fulltextCollection.getLastUpdateMetadata());
        assertEquals(LocalDateTime.of(2018, Month.OCTOBER,23,9,5,35,490),fulltextCollection.getLastUpdateFulltext());
        assertTrue(fulltextCollection.exists(ids.get(0)));
        assertTrue(fulltextCollection.exists(ids.get(1)));
        assertEquals(new Pair<LocalDateTime,LocalDateTime>(LocalDateTime.of(2018,Month.JULY,11,14,52,41,794),LocalDateTime.of(2018,Month.OCTOBER,23,9,5,35,490)),fulltextCollection.getLastUpdateDates(ids.get(0)));
        assertEquals(new Pair<LocalDateTime,LocalDateTime>(LocalDateTime.of(2018,Month.JULY,11,14,54,57,295),LocalDateTime.of(2018,Month.OCTOBER,23,9,0,52,508)),fulltextCollection.getLastUpdateDates(ids.get(1)));
        fulltextCollection.deleteDocuments(ids);
        fulltextCollection.commit();
    }

    @Test
    void test_metadata() throws IOException, SolrServerException {
        List<String> ids = new ArrayList<>();
        ids.add("/9200396/BibliographicResource_3000118435009");
        ids.add("/9200396/BibliographicResource_3000118436165");
        assertEquals(LocalDateTime.of(2018,Month.JULY,11,14,52,41,794000000),metadataCollection.getLastUpdateDate(ids.get(0)));
        assertEquals(LocalDateTime.of(2018,Month.JULY,11,14,54,57,295000000),metadataCollection.getLastUpdateDate(ids.get(1)));
        LocalDateTime date = LocalDateTime.of(2022,Month.APRIL,28,15,00,04,0);
        ZonedDateTime dateZone = ZonedDateTime.of(date, ZoneOffset.UTC);
        List<String> modified = metadataCollection.getDocumentsModifiedAfter(dateZone); //to check in Solr
    }


}
