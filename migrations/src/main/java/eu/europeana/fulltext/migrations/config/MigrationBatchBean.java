package eu.europeana.fulltext.migrations.config;

import static eu.europeana.fulltext.migrations.MigrationConstants.BATCH_THREAD_EXECUTOR;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
  public TaskExecutor migrationTaskExecutor() {
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(config.getBatchCorePoolSize());
    taskExecutor.setMaxPoolSize(config.getBatchMaxPoolSize());
    taskExecutor.setQueueCapacity(config.getBatchQueueSize());

    return taskExecutor;
  }
}
