package eu.europeana.fulltext.migrations;

import static eu.europeana.fulltext.migrations.MigrationConstants.BATCH_THREAD_EXECUTOR;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.migrations.config.MigrationAppSettings;
import eu.europeana.fulltext.migrations.repository.MigrationRepository;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class MigrationBatchConfig {

  private final JobBuilderFactory jobs;

  private final StepBuilderFactory steps;

  private final TaskExecutor migrationTaskExecutor;

  private final MigrationAppSettings appSettings;
  private final MigrationRepository repository;

  private final MigrationAnnoPageProcessor processor;
  private final MigrationAnnoPageWriter writer;

  private static final Logger logger = LogManager.getLogger(MigrationBatchConfig.class);

  /** SkipPolicy to ignore all failures when executing jobs, as they can be handled later */
  private static final SkipPolicy noopSkipPolicy = (Throwable t, int skipCount) -> true;

  public MigrationBatchConfig(
      JobBuilderFactory jobs,
      StepBuilderFactory steps,
      @Qualifier(BATCH_THREAD_EXECUTOR) TaskExecutor migrationTaskExecutor,
      MigrationAppSettings appSettings,
      MigrationRepository repository,
      MigrationAnnoPageProcessor processor,
      MigrationAnnoPageWriter writer) {
    this.jobs = jobs;
    this.steps = steps;
    this.migrationTaskExecutor = migrationTaskExecutor;
    this.appSettings = appSettings;
    this.repository = repository;
    this.processor = processor;
    this.writer = writer;
  }

  private ItemReader<AnnoPage> annoPageReader(MigrationJobMetadata jobMetadata) {
    return threadSafeReader(
        new MigrationAnnoPageReader(appSettings.getPageSize(), repository, jobMetadata));
  }

  /** Makes ItemReader thread-safe */
  private <T> SynchronizedItemStreamReader<T> threadSafeReader(ItemStreamReader<T> reader) {
    final SynchronizedItemStreamReader<T> synchronizedItemStreamReader =
        new SynchronizedItemStreamReader<>();
    synchronizedItemStreamReader.setDelegate(reader);
    return synchronizedItemStreamReader;
  }

  private MigrationProgressLogger progressLogger(MigrationJobMetadata jobMetadata) {
    return new MigrationProgressLogger(appSettings, jobMetadata, repository);
  }


  private Step migrateAnnoPageStep(MigrationJobMetadata jobMetadata) {
    return this.steps
        .get("migrateAnnoPageStep")
        .<AnnoPage, AnnoPage>chunk(appSettings.getPageSize())
        .reader(annoPageReader(jobMetadata))
        .processor(processor)
        .writer(writer)
        .faultTolerant()
        .skipPolicy(noopSkipPolicy)
        .listener(
            progressLogger(jobMetadata))
        .taskExecutor(migrationTaskExecutor)
        .throttleLimit(appSettings.getBatchThrottleLimit())
        .build();
  }


  private Job migrateAnnoPageJob() {
    MigrationJobMetadata jobMetadata = repository.getExistingMetadata();

    if (jobMetadata != null) {
      logger.info("Found existing job metadata. Will resume processing from: {}", jobMetadata);
    } else {
      logger.info("No existing metadata found. Starting processing from scratch¬");
    }

    jobMetadata = new MigrationJobMetadata(null, new AtomicLong());
    return jobs.get("migrateAnnoPageJob")
        .preventRestart()
        .start(migrateAnnoPageStep(jobMetadata))
        .build();
  }

  @Bean
  public JobExecution run(JobLauncher jobLauncher) {
    JobExecution jobExecution = null;
    try {
      JobParameters jobParameters = new JobParametersBuilder()
          .addLong("time", System.currentTimeMillis()).toJobParameters();

      jobExecution = jobLauncher.run(migrateAnnoPageJob(), jobParameters);
      logger.info("Exit Status: {}", jobExecution.getStatus());
    } catch (Exception e) {
      logger.error("Error running job {}", e.getMessage());
    }
    return jobExecution;
  }
}
