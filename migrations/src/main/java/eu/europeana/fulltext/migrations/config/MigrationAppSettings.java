package eu.europeana.fulltext.migrations.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:migrations.properties")
@PropertySource(value = "classpath:migrations.user.properties", ignoreResourceNotFound = true)
public class MigrationAppSettings {
  @Value("${mongo.connectionUrl}")
  private String mongoConnectionUrl;

  @Value("${mongo.fulltext.database}")
  private String fulltextDatabase;

  @Value("${mongo.batch.database}")
  private String batchDatabase;

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


  public String getMongoConnectionUrl() {
    return mongoConnectionUrl;
  }

  public String getFulltextDatabase() {
    return fulltextDatabase;
  }

  public String getBatchDatabase() {
    return batchDatabase;
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
}
