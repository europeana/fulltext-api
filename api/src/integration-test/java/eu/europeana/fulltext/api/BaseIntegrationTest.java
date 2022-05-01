package eu.europeana.fulltext.api;

import eu.europeana.fulltext.api.testutils.MongoContainer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.output.WaitingConsumer;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
public abstract class BaseIntegrationTest {
  private static final MongoContainer MONGO_CONTAINER;

  static {
    MONGO_CONTAINER =
        new MongoContainer("fulltext", "fulltext-write-batch")
            .withLogConsumer(new WaitingConsumer().andThen(new ToStringConsumer()));
    MONGO_CONTAINER.start();
  }

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("auth.enabled", () -> "false");
    registry.add("annosync.enabled", () -> "false");
    registry.add(
        // add compressor, so we can detect issues during integration tests
        "mongo.connectionUrl", () -> MONGO_CONTAINER.getConnectionUrl() + "?compressors=snappy");
    registry.add("mongo.fulltext.database", MONGO_CONTAINER::getFulltextDb);
    registry.add("mongo.batch.database", MONGO_CONTAINER::getBatchDb);
    // remove annotationId domain restriction for tests
    registry.add("annotations.id.hosts", () -> ".*");
  }
}
