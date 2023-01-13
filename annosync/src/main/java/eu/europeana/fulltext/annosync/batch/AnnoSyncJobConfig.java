package eu.europeana.fulltext.annosync.batch;

import eu.europeana.fulltext.AppConstants;
import eu.europeana.fulltext.annosync.batch.listener.AnnoSyncUpdateListener;
import eu.europeana.fulltext.annosync.batch.model.AnnoSyncJobMetadata;
import eu.europeana.fulltext.annosync.batch.processor.AnnotationProcessor;
import eu.europeana.fulltext.annosync.batch.reader.ItemReaderConfig;
import eu.europeana.fulltext.annosync.batch.repository.AnnoSyncJobMetadataRepo;
import eu.europeana.fulltext.annosync.batch.writer.AnnoPageDeprecationWriter;
import eu.europeana.fulltext.annosync.batch.writer.AnnoPageUpsertWriter;
import eu.europeana.fulltext.annosync.config.AnnoSyncSettings;
import eu.europeana.fulltext.annosync.service.EmailService;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.exception.AnnotationApiRequestException;
import eu.europeana.fulltext.exception.MongoConnnectionException;
import eu.europeana.fulltext.subtitles.external.AnnotationItem;
import java.time.Duration;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class AnnoSyncJobConfig {

  private static final Logger logger = LogManager.getLogger(AnnoSyncJobConfig.class);

  private static final String ANNO_SYNC_JOB = "synchroniseAnnoJob";

  private final AnnoSyncSettings appSettings;

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  private final ItemReaderConfig itemReaderConfig;

  private final AnnotationProcessor annotationProcessor;
  private final AnnoPageUpsertWriter annoPageWriter;
  private final AnnoPageDeprecationWriter annoPageDeletionWriter;

  private final AnnoSyncUpdateListener updateListener;

  private final TaskExecutor annoSyncTaskExecutor;

  private final AnnoSyncStats stats;
  private final EmailService emailService;

  private final AnnoSyncJobMetadataRepo annoSyncJobMetaRepository;

  public AnnoSyncJobConfig(
      AnnoSyncSettings appSettings,
      JobBuilderFactory jobBuilderFactory,
      StepBuilderFactory stepBuilderFactory,
      ItemReaderConfig itemReaderConfig,
      AnnotationProcessor annotationProcessor,
      AnnoPageUpsertWriter annoPageWriter,
      AnnoPageDeprecationWriter annoPageDeletionWriter,
      AnnoSyncUpdateListener updateListener,
      @Qualifier(AppConstants.ANNO_SYNC_TASK_EXECUTOR) TaskExecutor annoSyncTaskExecutor,
      AnnoSyncStats stats,
      EmailService emailService,
      AnnoSyncJobMetadataRepo annoSyncJobMetaRepository) {
    this.appSettings = appSettings;
    this.jobBuilderFactory = jobBuilderFactory;
    this.stepBuilderFactory = stepBuilderFactory;
    this.itemReaderConfig = itemReaderConfig;
    this.annotationProcessor = annotationProcessor;
    this.annoPageWriter = annoPageWriter;
    this.annoPageDeletionWriter = annoPageDeletionWriter;
    this.updateListener = updateListener;
    this.annoSyncTaskExecutor = annoSyncTaskExecutor;
    this.stats = stats;
    this.emailService = emailService;
    this.annoSyncJobMetaRepository = annoSyncJobMetaRepository;
  }

  private Step syncAnnotationsStep(Instant from, Instant to) {
    return this.stepBuilderFactory
        .get("synchroniseAnnoStep")
        .<AnnotationItem, AnnoPage>chunk(appSettings.getAnnotationItemsPageSize())
        .reader(itemReaderConfig.createAnnotationReader(from, to))
        .processor(annotationProcessor)
        .writer(annoPageWriter)
        .listener((ItemProcessListener<? super AnnotationItem, ? super AnnoPage>) updateListener)
        .faultTolerant()
        .retryLimit(appSettings.getRetryLimit())
        .retry(
            AnnotationApiRequestException.class) // retry if Annotaions Api is down for some reason
        .retry(MongoConnnectionException.class) // retry if MongoDb is down for some reason
        .skipLimit(appSettings.getSkipLimit())
        .skip(Exception.class)
        .taskExecutor(annoSyncTaskExecutor)
        .throttleLimit(appSettings.getAnnoSyncThrottleLimit())
        .build();
  }

  private Step deleteAnnotationsStep(Instant from, Instant to) {
    return this.stepBuilderFactory
        .get("deleteAnnotationsStep")
        .<String, String>chunk(appSettings.getAnnotationItemsPageSize())
        .reader(itemReaderConfig.createDeletedAnnotationReader(from, to))
        .writer(annoPageDeletionWriter)
        .faultTolerant()
        .retryLimit(appSettings.getRetryLimit())
        .retry(
            AnnotationApiRequestException.class) // retry if Annotaions Api is down for some reason
        .retry(MongoConnnectionException.class) // retry if MongoDb is down for some reason
        .skipLimit(appSettings.getSkipLimit())
        .skip(Exception.class)
        .taskExecutor(annoSyncTaskExecutor)
        .throttleLimit(appSettings.getAnnoSyncThrottleLimit())
        .build();
  }

  @Bean
  public Job syncAnnotations() {

    AnnoSyncJobMetadata jobMetadata = annoSyncJobMetaRepository.getMostRecentAnnoSyncMetadata();
    Instant from = Instant.EPOCH;
    Instant startTime = Instant.now();

    // take from value from previous run if it exists
    if (jobMetadata != null) {
      from = jobMetadata.getLastSuccessfulStartTime();
    } else {
      jobMetadata = new AnnoSyncJobMetadata();
    }

    jobMetadata.setLastSuccessfulStartTime(startTime);

    if (logger.isInfoEnabled()) {
      logger.info(
          "Starting annotation sync job. Fetching annotations from {} to {}",
          from,
          startTime);
    }

    return this.jobBuilderFactory
        .get(ANNO_SYNC_JOB)
        .start(initStats(stats, startTime))
        .next(syncAnnotationsStep(from, startTime))
        .next(deleteAnnotationsStep(from, startTime))
        .next(finishStats(stats, startTime))
        .next(sendSuccessEmailStep(from, startTime))
        .next(updateAnnoSyncJobMetadata(jobMetadata))
        .build();
  }

  private Step finishStats(AnnoSyncStats stats, Instant startTime) {
    return stepBuilderFactory
        .get("finishStatsStep")
        .tasklet(
            ((stepContribution, chunkContext) -> {
              stats.setElapsedTime(Duration.between(startTime, Instant.now()));
              return RepeatStatus.FINISHED;
            }))
        .build();
  }

  private Step initStats(AnnoSyncStats stats, Instant startTime) {
    return stepBuilderFactory
        .get("initStatsStep")
        .tasklet(
            ((stepContribution, chunkContext) -> {
              stats.reset();
              stats.setStartTime(startTime);
              return RepeatStatus.FINISHED;
            }))
        .build();
  }

  private Step updateAnnoSyncJobMetadata(AnnoSyncJobMetadata jobMetadata) {
    return stepBuilderFactory
        .get("updateJobMetadataStep")
        .tasklet(new AnnoSyncMetadataUpdaterTasklet(annoSyncJobMetaRepository, jobMetadata))
        .build();
  }

  private Step sendSuccessEmailStep(Instant from, Instant to) {
    return stepBuilderFactory
        .get("sendEmailStep")
        .tasklet(new MailSenderTasklet(stats, emailService, from, to, appSettings))
        .build();
  }
}
