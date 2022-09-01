package eu.europeana.fulltext.api.testutils;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

/** This class creates a Mongo container using the dockerfile in the docker-scripts directory. */
public class MongoContainer extends GenericContainer<MongoContainer> {

  private final String fulltextDb;
  private final String adminUsername = "admin";
  private final String adminPassword = "password";

  private final boolean useFixedPorts = false;

  /**
   * Creates a new Mongo container instance
   *
   * @param fulltextDb fulltext database
   */
  public MongoContainer(String fulltextDb) {

    this(
        new ImageFromDockerfile()
            // in test/resources directory
            .withFileFromClasspath("Dockerfile", "mongo-docker/Dockerfile")
            .withFileFromClasspath("init-mongo.sh", "mongo-docker/init-mongo.sh"),
        fulltextDb);
  }

  public MongoContainer(ImageFromDockerfile dockerImageName, String fulltextDb) {
    super(dockerImageName);

    if (useFixedPorts) {
      this.addFixedExposedPort(27018, 27017);
    } else {
      this.withExposedPorts(27017);
    }

    this.withEnv("ROOT_USERNAME", adminUsername)
        .withEnv("ROOT_PASSWORD", adminPassword)
        .withEnv("FULLTEXT_DB", fulltextDb);

    this.waitingFor(Wait.forLogMessage("(?i).*waiting for connections.*", 1));
    this.fulltextDb = fulltextDb;
  }

  public String getConnectionUrl() {
    if (!this.isRunning()) {
      throw new IllegalStateException("MongoDBContainer should be started first");
    } else {
      return String.format(
          "mongodb://%s:%s@%s:%d/",
          adminUsername, adminPassword, this.getContainerIpAddress(), this.getMappedPort(27017));
    }
  }

  public String getFulltextDb() {
    return fulltextDb;
  }

}
