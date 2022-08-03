package eu.europeana.fulltext.indexing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;
import java.io.IOException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.common.util.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class BasicTest extends AbstractIntegrationTest {

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
        fulltextCollection.deleteDocument(ids.get(0));
        fulltextCollection.deleteDocument(ids.get(1));
        assertFalse(fulltextCollection.existsByEuropeanaID(ids.get(0)));
        assertFalse(fulltextCollection.existsByEuropeanaID(ids.get(1)));
        fulltextCollection.setMetadata(ids.get(0),metadataCollection);
        fulltextCollection.setMetadata(ids.get(1),metadataCollection);
        assertTrue(fulltextCollection.checkMetadata(ids.get(0)));
        assertTrue(fulltextCollection.checkMetadata(ids.get(1)));
        fulltextCollection.setFulltext(ids.get(0));
        fulltextCollection.setFulltext(ids.get(1));
        assertEquals(LocalDateTime.of(2018, Month.JULY,11,14,54,57,295),fulltextCollection.getLastUpdateMetadata());
        assertEquals(LocalDateTime.of(2018, Month.OCTOBER,23,9,5,35,490),fulltextCollection.getLastUpdateFulltext());
        assertTrue(fulltextCollection.existsByEuropeanaID(ids.get(0)));
        assertTrue(fulltextCollection.existsByEuropeanaID(ids.get(1)));
        assertEquals(new Pair<LocalDateTime,LocalDateTime>(LocalDateTime.of(2018,Month.JULY,11,14,52,41,794),LocalDateTime.of(2018,Month.OCTOBER,23,9,5,35,490)),fulltextCollection.getLastUpdateDates(ids.get(0)));
        assertEquals(new Pair<LocalDateTime,LocalDateTime>(LocalDateTime.of(2018,Month.JULY,11,14,54,57,295),LocalDateTime.of(2018,Month.OCTOBER,23,9,0,52,508)),fulltextCollection.getLastUpdateDates(ids.get(1)));
        fulltextCollection.deleteDocument(ids.get(0));
        fulltextCollection.deleteDocument(ids.get(1));
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
        List<TupleStream> streams = metadataCollection.getDocumentsModifiedAfter(dateZone); //to check in Solr
        List<String> documents = metadataCollection.getDocumentsModifiedAfter(streams);
    }


    @Test
    public void syncFulltextTest() throws Exception {
        fulltextCollection.synchronizeFulltextContent(ZonedDateTime.ofInstant(Instant.EPOCH,ZoneOffset.UTC));
        //fulltextCollection.synchronizeFulltextContent();
        assertEquals(new ArrayList<String>(), fulltextCollection.isFulltextUpdated());
    }

    @Test
    public void syncMetadata() throws IOException, SolrServerException {
        fulltextCollection.synchronizeMetadataContent(ZonedDateTime.ofInstant(Instant.EPOCH,ZoneOffset.UTC));
    }

}
