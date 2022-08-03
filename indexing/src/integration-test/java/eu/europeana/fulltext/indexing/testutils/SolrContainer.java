package eu.europeana.fulltext.indexing.testutils;

import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

/** Creates a docker container for Solr using the dockerfile in docker-scripts directory
 *  It is capable of starting two containers one for Fulltext collection and one for metadata Collection on different ports
 **/
public class SolrContainer extends GenericContainer<SolrContainer> {

    private static final int DEFAULT_SOLR_PORT = 8983;
    private static final int SOLR_PORT = 8984;

    private final String fulltextCore;
    private final boolean useFixedPorts = false;

    public SolrContainer(String fulltextCore, boolean isFulltext) {
        this(
               isFulltext ?
                       new ImageFromDockerfile()
                        // in test/resources directory
                        .withFileFromClasspath("Dockerfile", "solr-docker/Dockerfile")
                        .withFileFromClasspath("solr-entrypoint.sh", "solr-docker/solr-entrypoint.sh")
                        .withFileFromClasspath("conf", "solr-docker/conf/")
                        .withFileFromClasspath("accent-map.txt", "solr-docker/conf/accent-map.txt")
                        .withFileFromClasspath("schema.xml", "solr-docker/conf/schema.xml")
                        .withFileFromClasspath("solrconfig.xml", "solr-docker/conf/solrconfig.xml")
                       :
                       new ImageFromDockerfile()
                               // in test/resources directory
                               .withFileFromClasspath("Dockerfile", "solr-docker/Dockerfile")
                               .withFileFromClasspath("solr-entrypoint.sh", "solr-docker/solr-entrypoint.sh")
                               .withFileFromClasspath("conf", "solr-docker/metadata-conf/")
                               .withFileFromClasspath("accent-map.txt", "solr-docker/conf/accent-map.txt") // using the same accept-map.txt
                               .withFileFromClasspath("schema.xml", "solr-docker/metadata-conf/schema.xml")
                               .withFileFromClasspath("solrconfig.xml", "solr-docker/metadata-conf/solrconfig.xml"),
                fulltextCore, isFulltext);
    }

    private SolrContainer(ImageFromDockerfile dockerImageName, String fulltextCore, boolean isFulltext) {
        super(dockerImageName);
        if (useFixedPorts) {
            if (isFulltext) {
                this.addFixedExposedPort(DEFAULT_SOLR_PORT, DEFAULT_SOLR_PORT);
            } else {
                this.addFixedExposedPort(SOLR_PORT, SOLR_PORT);
            }
        } else {
            if (isFulltext) {
                this.withExposedPorts(DEFAULT_SOLR_PORT);
            } else {
                this.withExposedPorts(SOLR_PORT);
            }
        }

        this.withEnv("FULLTEXT_INDEXING_CORE", fulltextCore);
        this.waitStrategy =
                new LogMessageWaitStrategy()
                        .withRegEx(".*o\\.e\\.j\\.s\\.Server Started.*")
                        .withStartupTimeout(Duration.of(60, SECONDS));

        this.fulltextCore = fulltextCore;
    }

    public String getFulltextConnectionUrl() {
        if (!this.isRunning()) {
            throw new IllegalStateException("Solr container should be started first");
        } else {
            return String.format(
                    "http://%s:%d/solr/%s",
                    this.getContainerIpAddress(),
                    this.getMappedPort(DEFAULT_SOLR_PORT),
                    fulltextCore);
        }
    }

    public String getMetadataConnectionUrl() {
        if (!this.isRunning()) {
            throw new IllegalStateException("Solr container should be started first");
        } else {
            return String.format(
                    "http://%s:%d/solr/%s",
                    this.getContainerIpAddress(),
                    this.getMappedPort(SOLR_PORT),
                    fulltextCore);
        }
    }


}
