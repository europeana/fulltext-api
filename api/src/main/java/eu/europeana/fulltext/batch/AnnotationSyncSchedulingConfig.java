package eu.europeana.fulltext.batch;

import static eu.europeana.fulltext.AppConstants.ANNO_SYNC_TASK_SCHEDULER;

import eu.europeana.fulltext.api.config.FTSettings;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;

/** Configures the scheduling of periodic Annotation synchronisation. */
@Configuration
public class AnnotationSyncSchedulingConfig implements InitializingBean {

  private final AnnotationSyncJobConfig annoSyncJobConfig;

  private final TaskScheduler annoSyncTaskScheduler;
  private static final Logger logger = LogManager.getLogger(AnnotationSyncSchedulingConfig.class);

  private final JobLauncher jobLauncher;
  private final int annoSyncInitialDelay;
  private final int annoSyncInterval;

  private final boolean annoSyncEnabled;

  public AnnotationSyncSchedulingConfig(
      AnnotationSyncJobConfig annoSyncJobConfig,
      @Qualifier(ANNO_SYNC_TASK_SCHEDULER) TaskScheduler annoSyncTaskScheduler,
      JobLauncher launcher,
      FTSettings appSettings) {
    this.annoSyncJobConfig = annoSyncJobConfig;
    this.annoSyncTaskScheduler = annoSyncTaskScheduler;
    this.jobLauncher = launcher;
    this.annoSyncInitialDelay = appSettings.getAnnoSyncInitialDelay();
    this.annoSyncInterval = appSettings.getAnnoSyncInterval();
    this.annoSyncEnabled = appSettings.isAnnoSyncEnabled();
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (!annoSyncEnabled) {
      logger.warn(
          "Annotations syncing not enabled. Annotations will not be retrieved automatically");
      return;
    }

    if (logger.isInfoEnabled()) {
      logger.info(
          "AnnoSync scheduling initialized – initialDelay: {}; interval: {}",
          toMinutesAndSeconds(annoSyncInitialDelay),
          toMinutesAndSeconds(annoSyncInterval));
    }

    schedulePeriodicAnnoSync();
  }

  private void schedulePeriodicAnnoSync() {
    annoSyncTaskScheduler.scheduleWithFixedDelay(
        this::runScheduledAnnoSyncJob,
        Instant.now().plusSeconds(annoSyncInitialDelay),
        Duration.ofSeconds(annoSyncInterval));
  }

  /** Periodically run full entity updates. */
  void runScheduledAnnoSyncJob() {
    logger.info("Triggering scheduled AnnoSync job");
    try {
      String startTimeJobParam = "startTime";
      jobLauncher.run(
          annoSyncJobConfig.syncAnnotations(),
          new JobParametersBuilder()
              .addDate(startTimeJobParam, Date.from(Instant.now()))
              .toJobParameters());
    } catch (Exception e) {
      logger.warn("Error running AnnoSync job", e);
    }
  }
  /** Converts Seconds to "x min, y sec" */
  private String toMinutesAndSeconds(long seconds) {
    return String.format(
        "%d min, %d sec",
        TimeUnit.SECONDS.toMinutes(seconds),
        seconds - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds)));
  }
}
