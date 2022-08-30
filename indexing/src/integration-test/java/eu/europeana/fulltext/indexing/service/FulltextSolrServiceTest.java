package eu.europeana.fulltext.indexing.service;

import static org.junit.jupiter.api.Assertions.*;

import eu.europeana.fulltext.indexing.solr.FulltextSolrService;
import java.time.Instant;
import java.util.Optional;
import org.apache.solr.client.solrj.response.schema.SchemaRepresentation;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Excluded from automated runs")
class FulltextSolrServiceTest {

  @Autowired private FulltextSolrService solrService;

  @Test
  void shouldFetchLastModifiedDate() throws Exception {
    Optional<Instant> lastUpdateTime = solrService.getLastUpdateTime();
    assertTrue(lastUpdateTime.isPresent());
  }

  @Test
  void shouldFetchSchema() throws Exception {
    SchemaRepresentation schema = solrService.getSchema();

    assertNotNull(schema);
  }
}
