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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import java.util.Arrays;

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

  @Bean
  public Datastore fulltextDatastore() {
    logger.info("Configuring fulltext database: {}", fulltextDatabase);
    return Morphia.createDatastore(
        MongoClients.create(mongoConnectionUrl), fulltextDatabase, MAPPER_OPTIONS);
  }


  @Bean(METADATA_SOLR_BEAN)
  public SolrClient metadataSolrClient(){
    logger.info("Configuring metadata solr client: {}", metadataSolrUrl);
    //return new Http2SolrClient.Builder(metadataSolrUrl).build();
    return  new CloudSolrClient.Builder(Arrays.asList(metadataSolrUrl)).build();
  }

  @Bean(FULLTEXT_SOLR_BEAN)
  public SolrClient fulltextSolrClient(){
    logger.info("Configuring fulltext solr client: {}", fulltextSolrUrl);
    //return new Http2SolrClient.Builder(fulltextSolrUrl).build();
    return  new CloudSolrClient.Builder(Arrays.asList(fulltextSolrUrl)).build();
  }
}
