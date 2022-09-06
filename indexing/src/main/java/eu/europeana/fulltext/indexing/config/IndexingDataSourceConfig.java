package eu.europeana.fulltext.indexing.config;

import static eu.europeana.fulltext.indexing.IndexingConstants.FULLTEXT_SOLR_BEAN;
import static eu.europeana.fulltext.indexing.IndexingConstants.METADATA_SOLR_BEAN;
import static eu.europeana.fulltext.util.MorphiaUtils.MAPPER_OPTIONS;

import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import eu.europeana.fulltext.entity.FulltextPackageMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IndexingDataSourceConfig {
  private static final Logger LOG = LogManager.getLogger(IndexingDataSourceConfig.class);

  private final IndexingAppSettings settings;

  public IndexingDataSourceConfig(IndexingAppSettings settings) {
    this.settings = settings;
  }

  @Bean
  public Datastore fulltextDatastore() {
    LOG.info("Configuring fulltext database: {}", settings.getFulltextDatabase());

    var datastore = Morphia.createDatastore(
        MongoClients.create(settings.getMongoConnectionUrl()),
        settings.getFulltextDatabase(),
        MAPPER_OPTIONS);

    if (settings.ensureFulltextIndices()) {
      // Create indices for Entity classes.(Just for Testing!!)
      datastore.getMapper().mapPackage(FulltextPackageMapper.class.getPackageName());
      datastore.ensureIndexes();
    }

    return datastore;
  }

  // TODO: maybe replace by metis solr utils
  /**
   * Create metadata client from solr node directly because it is expected it will be only one node,
   * and we need this information to get the cores
   *
   * @return SolrClient client for metadata collection
   */
  @Bean(METADATA_SOLR_BEAN)
  public CloudSolrClient metadataSolrClient() {
    LOG.info("Configuring metadata solr client: {}", settings.getMetadataSolrUrl());
    CloudSolrClient client =
        new CloudSolrClient.Builder(List.of(settings.getMetadataSolrUrl())).build();
    client.setDefaultCollection(settings.getMetadataCollection());
    return client;
  }

  /**
   * Create fulltext client from solr nodes (ideally it should be created from zookeeper nodes
   * because it is expected it will be more than one node and it is more efficient but I sometimes
   * get problems with the connection this way)
   *
   * @return Solr client for fulltext
   */
  @Bean(FULLTEXT_SOLR_BEAN)
  public SolrClient fulltextSolrClient() {
    if (StringUtils.isNotBlank(settings.getFulltextZkUrl())) {
      return initSolrCloudClient();
    } else {
      return initSolrClient();
    }
  }

  private SolrClient initSolrClient() {
    String fulltextSolrUrl = settings.getFulltextSolrUrl();

    LOG.info("Configuring Fulltext solr client: {}", fulltextSolrUrl);
    CloudSolrClient client =
        new CloudSolrClient.Builder(List.of(fulltextSolrUrl)).build();
    client.setDefaultCollection(settings.getFulltextCollection());
    return client;
  }

  private SolrClient initSolrCloudClient() {
    LOG.info(
        "Configuring indexing solr client with the zookeperurls: {} and collection: {}",
        settings.getFulltextZkUrl(),
        settings.getFulltextCollection());

    String[] solrZookeeperUrls = settings.getFulltextZkUrl().trim().split(",");

    CloudSolrClient client =
        new CloudSolrClient.Builder(Arrays.asList(solrZookeeperUrls), Optional.empty()).build();

    client.setDefaultCollection(settings.getFulltextCollection());
    return client;
  }
}
