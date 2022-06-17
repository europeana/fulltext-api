package eu.europeana.fulltext.indexing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
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
     void test() throws IOException, SolrServerException {
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
        Pair<LocalDateTime,LocalDateTime> dates = fulltextCollection.getLastUpdateDates(ids.get(0));
        fulltextCollection.deleteDocuments(ids);
        fulltextCollection.commit();
    }


}
