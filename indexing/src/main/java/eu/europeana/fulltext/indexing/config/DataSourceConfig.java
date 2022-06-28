package eu.europeana.fulltext.indexing.config;

import static eu.europeana.fulltext.indexing.Constants.FULLTEXT_SOLR_BEAN;
import static eu.europeana.fulltext.indexing.Constants.METADATA_SOLR_BEAN;
import static eu.europeana.fulltext.util.MorphiaUtils.MAPPER_OPTIONS;

import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
//import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Configuration
@PropertySource("classpath:indexing.properties")
@PropertySource(value = "classpath:indexing.user.properties", ignoreResourceNotFound = true)
public class DataSourceConfig {
  private static final Logger logger = LogManager.getLogger(DataSourceConfig.class);

  @Value("${mongo.connectionUrl}")
  private String mongoConnectionUrl;

  @Value("${mongo.fulltext.database}")
  private String fulltextDatabase;

  @Value("${solr.metadata.url}")
  private String metadataSolrUrl;

  @Value("${solr.fulltext.url}")
  private String fulltextSolrUrl;

  @Value("${solr.fulltext.zk.url}")
  private String fulltextZkUrl;

  @Value("${solr.metadata.collection}")
  private String metadataCollection;

  @Value("${solr.fulltext.collection}")
  private String fulltextCollection;

  @Bean
  public Datastore fulltextDatastore() {
    logger.info("Configuring fulltext database: {}", fulltextDatabase);
    return Morphia.createDatastore(
        MongoClients.create(mongoConnectionUrl), fulltextDatabase, MAPPER_OPTIONS);
  }

  //TODO: maybe replace by metis solr utils
  /**
   * Create metadata client from solr node directly because it is expected it will be only one node, and we need this information to get the cores
   * @return
   */
  @Bean(METADATA_SOLR_BEAN)
  public CloudSolrClient metadataSolrClient(){
    logger.info("Configuring metadata solr client: {}", metadataSolrUrl);
    // new Http2SolrClient.Builder(metadataSolrUrl).build();
    CloudSolrClient client = new CloudSolrClient.Builder(Arrays.asList(metadataSolrUrl)).build();
    client.setDefaultCollection(metadataCollection);
    return client;
  }

  //TODO: maybe replace by metis solr utils
  /**
   * Create fulltext client from solr nodes (ideally it should be created from zookeeper nodes because it is expected it will be more than one node and it is more efficient but I sometimes get problems with the connection this way
   * @return
   */
  @Bean(FULLTEXT_SOLR_BEAN)
  public CloudSolrClient fulltextSolrClient(){
    logger.info("Configuring fulltext solr client: {}", fulltextSolrUrl);
    //return new Http2SolrClient.Builder(fulltextSolrUrl).build();
    //CloudSolrClient client = new CloudSolrClient.Builder(Arrays.asList(fulltextZkUrl), java.util.Optional.empty()).build(); I have trouble sometimes connecting to Zookeeper
    CloudSolrClient client = new CloudSolrClient.Builder(Arrays.asList(fulltextSolrUrl)).build();
     client.setDefaultCollection(fulltextCollection);
    return  client;
  }
}
