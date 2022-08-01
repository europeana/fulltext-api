package eu.europeana.fulltext.indexing.config;

import static eu.europeana.fulltext.indexing.Constants.FULLTEXT_SOLR_BEAN;
import static eu.europeana.fulltext.indexing.Constants.METADATA_SOLR_BEAN;
import static eu.europeana.fulltext.util.MorphiaUtils.MAPPER_OPTIONS;

import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Arrays;

@Configuration
@PropertySource("classpath:indexing.properties")
@PropertySource(value = "classpath:indexing.user.properties", ignoreResourceNotFound = true)
public class DataSourceConfig {
  private static final Logger LOG = LogManager.getLogger(DataSourceConfig.class);

  @Value("${mongo.connectionUrl}")
  private String mongoConnectionUrl;

  @Value("${mongo.fulltext.database}")
  private String fulltextDatabase;

  @Value("${solr.fulltext.url}")
  private String fulltextSolrUrl;

  @Value("${solr.fulltext.collection}")
  private String fulltextCollection;

  @Value("${solr.metadata.url}")
  private String metadataSolrUrl;

  @Value("${solr.metadata.collection}")
  private String metadataCollection;

  @Value("${solr.fulltext.zk.url}")
  private String fulltextZkUrl;

  @Bean
  public Datastore fulltextDatastore() {
    LOG.info("Configuring fulltext database: {}", fulltextDatabase);
    return Morphia.createDatastore(
        MongoClients.create(mongoConnectionUrl), fulltextDatabase, MAPPER_OPTIONS);
  }

  //TODO: maybe replace by metis solr utils
  /**
   * Create metadata client from solr node directly because it is expected it will be only one node,
   * and we need this information to get the cores
   * @return SolrClient client for metadata collection
   */
  @Bean(METADATA_SOLR_BEAN)
  public CloudSolrClient metadataSolrClient(){
    LOG.info("Configuring metadata solr client: {}", metadataSolrUrl);
    CloudSolrClient client = new CloudSolrClient.Builder(Arrays.asList(metadataSolrUrl)).build();
    client.setDefaultCollection(metadataCollection);
    return client;
  }

  //TODO: maybe replace by metis solr utils
  /**
   * Create fulltext client from solr nodes
   * (ideally it should be created from zookeeper nodes because it is expected it will be more than one node
   * and it is more efficient but I sometimes get problems with the connection this way
   *
   * @return Solr client for fulltext
   */
  @Bean(FULLTEXT_SOLR_BEAN)
  public CloudSolrClient fulltextSolrClient(){
    LOG.info("Configuring fulltext solr client: {}", fulltextSolrUrl);
    // There is some issue connecting with zookeeper reported by Monica
    if (StringUtils.isNotEmpty(fulltextZkUrl)) {
      return new CloudSolrClient.Builder(Arrays.asList(fulltextZkUrl), java.util.Optional.empty()).build();
    }
    CloudSolrClient client = new CloudSolrClient.Builder(Arrays.asList(fulltextSolrUrl)).build();
    client.setDefaultCollection(fulltextCollection);
    return  client;
  }
}
