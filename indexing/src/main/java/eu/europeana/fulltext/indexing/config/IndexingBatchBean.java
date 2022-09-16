package eu.europeana.fulltext.indexing.config;

import static eu.europeana.fulltext.indexing.IndexingConstants.BATCH_THREAD_EXECUTOR;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class IndexingBatchBean {
  private final IndexingAppSettings config;

  public IndexingBatchBean(IndexingAppSettings config) {
    this.config = config;
  }

  /** Task executor used by the Spring Batch step for multi-threading */
  @Bean(BATCH_THREAD_EXECUTOR)
  public TaskExecutor indexingThreadExecutor() {
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(config.getBatchCorePoolSize());
    taskExecutor.setMaxPoolSize(config.getBatchMaxPoolSize());
    taskExecutor.setQueueCapacity(config.getBatchQueueSize());

    return taskExecutor;
  }
}
