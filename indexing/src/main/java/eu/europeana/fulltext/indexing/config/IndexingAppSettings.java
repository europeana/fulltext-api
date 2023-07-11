package eu.europeana.fulltext.indexing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:indexing.properties")
@PropertySource(value = "classpath:indexing.user.properties", ignoreResourceNotFound = true)
public class IndexingAppSettings {

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

  @Value("${solr.fulltext.zk.url:}")
  private String fulltextZkUrl;

  @Value("${batch.executor.corePool: 5}")
  private int batchCorePoolSize;

  @Value("${batch.executor.maxPool: 10}")
  private int batchMaxPoolSize;

  @Value("${batch.throttleLimit: 5}")
  private int batchThrottleLimit;

  @Value("${batch.executor.queueSize: 5}")
  private int batchQueueSize;

  @Value("${batch.pageSize: 100}")
  private int pageSize;

  @Value("${batch.metadataSync.pageSize: 5000}")
  private int metadataSolrSyncPageSize;

  @Value("${mongo.fulltext.ensureIndices: false}")
  private boolean ensureFulltextIndices;

  @Value("${batch.skipLimit: 500}")
  private int skipLimit;

  @Value("${batch.retryLimit: 3}")
  private int retryLimit;

  @Value("${batch.fulltext.commitWithinMs: 30000}")
  private int commitWithinMs;

  public String getMongoConnectionUrl() {
    return mongoConnectionUrl;
  }

  public String getFulltextDatabase() {
    return fulltextDatabase;
  }

  public String getFulltextSolrUrl() {
    return fulltextSolrUrl;
  }

  public String getFulltextCollection() {
    return fulltextCollection;
  }

  public String getMetadataSolrUrl() {
    return metadataSolrUrl;
  }

  public String getMetadataCollection() {
    return metadataCollection;
  }

  public String getFulltextZkUrl() {
    return fulltextZkUrl;
  }

  public int getBatchCorePoolSize() {
    return batchCorePoolSize;
  }

  public int getBatchMaxPoolSize() {
    return batchMaxPoolSize;
  }

  public int getBatchQueueSize() {
    return batchQueueSize;
  }

  public int getBatchThrottleLimit() {
    return batchThrottleLimit;
  }

  public int getBatchPageSize() {
    return pageSize;
  }

  public int getMetadataSolrSyncPageSize() {
    return metadataSolrSyncPageSize;
  }

  public boolean ensureFulltextIndices() {
    return ensureFulltextIndices;
  }

  public int getSkipLimit() {
    return skipLimit;  }

  public int getRetryLimit() {
    return retryLimit;
  }

  public int getCommitWithinMs() {
    return commitWithinMs;
  }
}
