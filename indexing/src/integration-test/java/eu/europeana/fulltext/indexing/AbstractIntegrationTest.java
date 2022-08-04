package eu.europeana.fulltext.indexing;

import eu.europeana.fulltext.indexing.testutils.MongoContainer;
import eu.europeana.fulltext.indexing.testutils.SolrContainer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;

import javax.xml.bind.JAXBContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.output.WaitingConsumer;

@AutoConfigureMockMvc
@DirtiesContext
public class AbstractIntegrationTest {

    private static final Logger logger = LogManager.getLogger(AbstractIntegrationTest.class);
    private static final MongoContainer MONGO_CONTAINER;
    private static final SolrContainer SOLR_CONTAINER_FULLTEXT;
    private static final SolrContainer SOLR_CONTAINER_METADATA;

    @Autowired protected JAXBContext jaxbContext;

    static {
        MONGO_CONTAINER =
                new MongoContainer("fulltext", "fulltext-write-batch")
                        .withLogConsumer(new WaitingConsumer().andThen(new ToStringConsumer()));
        MONGO_CONTAINER.start();

        SOLR_CONTAINER_FULLTEXT =
                new SolrContainer("fulltext", true)
                        .withLogConsumer(new WaitingConsumer().andThen(new ToStringConsumer()));

        SOLR_CONTAINER_FULLTEXT.start();

        SOLR_CONTAINER_METADATA =
                new SolrContainer("search_production", false)
                        .withLogConsumer(new WaitingConsumer().andThen(new ToStringConsumer()));

        SOLR_CONTAINER_METADATA.start();
    }


    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add(
                // add compressor, so we can detect issues during integration tests
                "mongo.connectionUrl", () -> MONGO_CONTAINER.getConnectionUrl() + "?compressors=snappy");
        registry.add("mongo.fulltext.database", MONGO_CONTAINER::getFulltextDb);
        registry.add("mongo.batch.database", MONGO_CONTAINER::getBatchDb);

        registry.add("solr.fulltext.url", SOLR_CONTAINER_FULLTEXT::getFulltextConnectionUrl);
        registry.add("solr.metadata.url", SOLR_CONTAINER_METADATA::getMetadataConnectionUrl);

        logger.info("MONGO_CONTAINER : {}", MONGO_CONTAINER.getConnectionUrl());
        logger.info("SOLR_CONTAINER_FULLTEXT : {}", SOLR_CONTAINER_FULLTEXT.getFulltextConnectionUrl());
        logger.info("SOLR_CONTAINER_METADATA : {}", SOLR_CONTAINER_METADATA.getMetadataConnectionUrl());

    }

}
