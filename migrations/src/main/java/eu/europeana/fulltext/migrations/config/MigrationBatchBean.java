package eu.europeana.fulltext.migrations.config;

import static eu.europeana.fulltext.migrations.MigrationConstants.BATCH_DATASTORE_BEAN;
import static eu.europeana.fulltext.migrations.MigrationConstants.BATCH_THREAD_EXECUTOR;

import dev.morphia.Datastore;
import eu.europeana.batch.config.MongoBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class MigrationBatchBean {

  private final MigrationAppSettings config;

  public MigrationBatchBean(MigrationAppSettings config) {
    this.config = config;
  }

  /** Task executor used by the Spring Batch step for multi-threading */
  @Bean(BATCH_THREAD_EXECUTOR)
  public TaskExecutor annoSyncTaskExecutor() {
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(config.getBatchCorePoolSize());
    taskExecutor.setMaxPoolSize(config.getBatchMaxPoolSize());
    taskExecutor.setQueueCapacity(config.getBatchQueueSize());

    return taskExecutor;
  }

  /**
   * Configures Spring Batch to use Mongo
   *
   * @param datastore Morphia datastore for Spring Batch
   * @return BatchConfigurer instance
   */
  @Bean
  public BatchConfigurer mongoBatchConfigurer(
      @Qualifier(BATCH_DATASTORE_BEAN) Datastore datastore) {

    return new MongoBatchConfigurer(datastore, new SyncTaskExecutor());
  }
}
