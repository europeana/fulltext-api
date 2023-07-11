package eu.europeana.fulltext.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.api.testutils.MongoContainer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.output.WaitingConsumer;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
public abstract class BaseIntegrationTest {
  private static final MongoContainer MONGO_CONTAINER;
  protected static final String BASE_SERVICE_URL = "/presentation";

  @Autowired protected WebApplicationContext webApplicationContext;
  @Autowired protected FTService ftService;
  @Autowired protected ObjectMapper mapper;

  protected MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    this.ftService.deleteAll();
  }

  static {
    MONGO_CONTAINER =
        new MongoContainer("fulltext")
            .withLogConsumer(new WaitingConsumer().andThen(new ToStringConsumer()));
    MONGO_CONTAINER.start();
  }

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("auth.enabled", () -> "false");
    registry.add(
        // add compressor, so we can detect issues during integration tests
        "mongo.connectionUrl", () -> MONGO_CONTAINER.getConnectionUrl() + "?compressors=snappy");
    registry.add("mongo.fulltext.database", MONGO_CONTAINER::getFulltextDb);
    // remove annotationId domain restriction for tests
    registry.add("annotations.id.hosts", () -> ".*");
  }
}
