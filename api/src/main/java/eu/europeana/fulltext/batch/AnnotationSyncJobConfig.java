package eu.europeana.fulltext.batch;

import static eu.europeana.fulltext.AppConstants.ANNO_SYNC_TASK_EXECUTOR;
import static eu.europeana.fulltext.batch.BatchUtils.ANNO_SYNC_JOB;

import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.service.EmailService;
import eu.europeana.fulltext.batch.listener.AnnoSyncUpdateListener;
import eu.europeana.fulltext.batch.processor.AnnotationProcessor;
import eu.europeana.fulltext.batch.reader.ItemReaderConfig;
import eu.europeana.fulltext.batch.writer.AnnoPageDeletionWriter;
import eu.europeana.fulltext.batch.writer.AnnoPageUpsertWriter;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltext.subtitles.external.AnnotationItem;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
@EnableBatchProcessing
public class AnnotationSyncJobConfig {

  private static final Logger logger = LogManager.getLogger(AnnotationSyncJobConfig.class);

  private final FTSettings appSettings;

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final JobExplorer jobExplorer;

  private final ItemReaderConfig itemReaderConfig;

  private final AnnotationProcessor annotationProcessor;
  private final AnnoPageUpsertWriter annoPageWriter;
  private final AnnoPageDeletionWriter annoPageDeletionWriter;

  private final AnnoSyncUpdateListener updateListener;

  private final TaskExecutor annoSyncTaskExecutor;

  private final AnnoSyncStats stats;
  private final EmailService emailService;

  /** SkipPolicy to ignore all failures when executing jobs, as they can be handled later */
  private static final SkipPolicy noopSkipPolicy = (Throwable t, int skipCount) -> true;

  public AnnotationSyncJobConfig(
      FTSettings appSettings,
      JobBuilderFactory jobBuilderFactory,
      StepBuilderFactory stepBuilderFactory,
      JobExplorer jobExplorer,
      ItemReaderConfig itemReaderConfig,
      AnnotationProcessor annotationProcessor,
      AnnoPageUpsertWriter annoPageWriter,
      AnnoPageDeletionWriter annoPageDeletionWriter,
      AnnoSyncUpdateListener updateListener,
      @Qualifier(ANNO_SYNC_TASK_EXECUTOR) TaskExecutor annoSyncTaskExecutor,
      AnnoSyncStats stats, EmailService emailService) {
    this.appSettings = appSettings;
    this.jobBuilderFactory = jobBuilderFactory;
    this.stepBuilderFactory = stepBuilderFactory;
    this.jobExplorer = jobExplorer;
    this.itemReaderConfig = itemReaderConfig;
    this.annotationProcessor = annotationProcessor;
    this.annoPageWriter = annoPageWriter;
    this.annoPageDeletionWriter = annoPageDeletionWriter;
    this.updateListener = updateListener;
    this.annoSyncTaskExecutor = annoSyncTaskExecutor;
    this.stats = stats;
    this.emailService = emailService;
  }

  private Step syncAnnotationsStep(Instant from, Instant to) {
    return this.stepBuilderFactory
        .get("synchroniseAnnoStep")
        .<AnnotationItem, TranslationAnnoPage>chunk(appSettings.getAnnotationItemsPageSize())
        .reader(itemReaderConfig.createAnnotationReader(from, to))
        .processor(annotationProcessor)
        .writer(annoPageWriter)
        .faultTolerant()
        .skipPolicy(noopSkipPolicy)
        .taskExecutor(annoSyncTaskExecutor)
        .throttleLimit(appSettings.getAnnoSyncThrottleLimit())
        .listener(updateListener)
        .build();
  }

  private Step deleteAnnotationsStep(Instant from, Instant to) {
    return this.stepBuilderFactory
        .get("deleteAnnotationsStep")
        .<String, String>chunk(appSettings.getAnnotationItemsPageSize())
        .reader(itemReaderConfig.createDeletedAnnotationReader(from, to))
        .writer(annoPageDeletionWriter)
        .faultTolerant()
        .skipPolicy(noopSkipPolicy)
        .taskExecutor(annoSyncTaskExecutor)
        .throttleLimit(appSettings.getAnnoSyncThrottleLimit())
        .build();
  }

  public Job syncAnnotations() {
    Instant from = BatchUtils.getMostRecentSuccessfulStartTime(jobExplorer);
    Instant to = Instant.now();

    if (logger.isInfoEnabled()) {
      String fromLogString = from == null ? "*" : from.toString();
      logger.info(
          "Starting annotation sync job. Fetching annotations from {} to {}", fromLogString, to);
    }

    // reset stats before each job run
    stats.reset();

    return this.jobBuilderFactory
        .get(ANNO_SYNC_JOB)
        .start(syncAnnotationsStep(from, to))
        .next(deleteAnnotationsStep(from, to))
        .next(sendSuccessEmailStep(from, to))
        .build();
  }

  private Step sendSuccessEmailStep(Instant from, Instant to) {
    return stepBuilderFactory
        .get("sendEmailStep")
        .tasklet(new MailSenderTasklet(stats, emailService, from, to, appSettings))
        .build();
  }
}
