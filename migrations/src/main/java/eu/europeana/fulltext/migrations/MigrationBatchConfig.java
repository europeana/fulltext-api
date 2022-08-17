package eu.europeana.fulltext.migrations;

import static eu.europeana.fulltext.migrations.MigrationConstants.BATCH_THREAD_EXECUTOR;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.migrations.config.MigrationAppSettings;
import eu.europeana.fulltext.migrations.listener.MigrationProgressListener;
import eu.europeana.fulltext.migrations.listener.MigrationSkipListener;
import eu.europeana.fulltext.migrations.model.MigrationJobMetadata;
import eu.europeana.fulltext.migrations.processor.MigrationAnnoPageProcessor;
import eu.europeana.fulltext.migrations.reader.MigrationAnnoPageReader;
import eu.europeana.fulltext.migrations.repository.MigrationRepository;
import eu.europeana.fulltext.migrations.writer.MigrationAnnoPageWriter;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
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
  private final MigrationSkipListener skipListener;

  private static final Logger logger = LogManager.getLogger(MigrationBatchConfig.class);
  private final MigrationAnnoPageWriter writer;

  public MigrationBatchConfig(
      JobBuilderFactory jobs,
      StepBuilderFactory steps,
      @Qualifier(BATCH_THREAD_EXECUTOR) TaskExecutor migrationTaskExecutor,
      MigrationAppSettings appSettings,
      MigrationRepository repository,
      MigrationAnnoPageProcessor processor,
      MigrationSkipListener skipListener,
      MigrationAnnoPageWriter writer) {
    this.jobs = jobs;
    this.steps = steps;
    this.migrationTaskExecutor = migrationTaskExecutor;
    this.appSettings = appSettings;
    this.repository = repository;
    this.processor = processor;
    this.skipListener = skipListener;
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

  private MigrationProgressListener reportingListener(MigrationJobMetadata jobMetadata) {
    return new MigrationProgressListener(appSettings, jobMetadata, repository);
  }

  private Step migrateAnnoPageStep(MigrationJobMetadata jobMetadata) {
    return this.steps
        .get("migrateAnnoPageStep")
        .<AnnoPage, AnnoPage>chunk(appSettings.getPageSize())
        .reader(annoPageReader(jobMetadata))
        .processor(processor)
        .writer(writer)
        .faultTolerant()
        // skip all exceptions up to the configurable limit
        .skip(Exception.class)
        .skipLimit(appSettings.getSkipLimit())
        .listener((ItemReadListener<? super AnnoPage>) reportingListener(jobMetadata))
        .listener(skipListener)
        .taskExecutor(migrationTaskExecutor)
        .throttleLimit(appSettings.getBatchThrottleLimit())
        .build();
  }

  @Bean
  private Job migrateAnnoPageJob() {
    MigrationJobMetadata jobMetadata = repository.getExistingMetadata();

    if (jobMetadata != null) {
      logger.info("Found existing job metadata. Will resume processing from: {}", jobMetadata);
    } else {
      logger.info("No existing metadata found. Starting processing from scratch");
      jobMetadata = new MigrationJobMetadata(null, new AtomicLong());
    }

    return jobs.get("migrateAnnoPageJob")
        .preventRestart()
        .incrementer(
            // ensure each job run is unique
            (JobParameters p) ->
                new JobParametersBuilder().addDate("startTime", new Date()).toJobParameters())
        .start(migrateAnnoPageStep(jobMetadata))
        .build();
  }
}
