package eu.europeana.fulltext.migrations.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:migrations.properties")
@PropertySource(value = "classpath:migrations.user.properties", ignoreResourceNotFound = true)
public class MigrationAppSettings {
  @Value("${mongo.source.connectionUrl}")
  private String mongoSrcConnectionUrl;

  @Value("${mongo.source.fulltext.database}")
  private String fulltextSrcDatabase;

  @Value("${mongo.dest.connectionUrl:}")
  private String mongoDestConnectionUrl;

  @Value("${mongo.dest.fulltext.database:}")
  private String fulltextDestDatabase;

  @Value("${mongo.useSameDb}")
  private boolean useSameDb;


  @Value("${batch.pageSize: 100}")
  private int pageSize;

  @Value("${batch.executor.corePool: 5}")
  private int batchCorePoolSize;

  @Value("${batch.executor.maxPool: 10}")
  private int batchMaxPoolSize;

  @Value("${batch.throttleLimit: 5}")
  private int batchThrottleLimit;

  @Value("${batch.step.executor.queueSize: 5}")
  private int batchQueueSize;

  @Value("${batch.loggingInterval: 1000}")
  private int loggingInterval;

  @Value("${batch.skipLimit: 500}")
  private int skipLimit;

  /**
   * Threshold for AnnoPage annotation count above which annotations are updated in chunks,
   *
   * <p>Some AnnoPages have >70k annotations, which results in a large json size when we update
   * everything in one query. Mongo doesn't like that.
   */
  @Value("${batch.tooManyAnnotationsThreshold: 5000}")
  private int tooManyAnnotationsThreshold;

  @Value("${mongo.fulltext.totalCount:0}")
  private long totalCount;


  public String getMongoSrcConnectionUrl() {
    return mongoSrcConnectionUrl;
  }

  public String getFulltextSrcDatabase() {
    return fulltextSrcDatabase;
  }

  public int getPageSize() {
    return pageSize;
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

  public int getLoggingInterval() {
    return loggingInterval;
  }

  public int getSkipLimit() {
    return skipLimit;
  }

  public long getTotalCount(){
    return totalCount;
  }

  public int getTooManyAnnotationsThreshold() {
    return tooManyAnnotationsThreshold;
  }

  public String getMongoDestConnectionUrl() {
    return mongoDestConnectionUrl;
  }

  public String getFulltextDestDatabase() {
    return fulltextDestDatabase;
  }

  public boolean useSameDb() {
    return useSameDb;
  }
}
