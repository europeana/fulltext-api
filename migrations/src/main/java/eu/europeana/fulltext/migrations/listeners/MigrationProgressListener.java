package eu.europeana.fulltext.migrations.listeners;

import static eu.europeana.fulltext.util.GeneralUtils.getAnnoPageObjectIds;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.migrations.config.MigrationAppSettings;
import eu.europeana.fulltext.migrations.model.MigrationJobMetadata;
import eu.europeana.fulltext.migrations.repository.MigrationRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.listener.ItemListenerSupport;

public class MigrationProgressListener extends ItemListenerSupport<AnnoPage, AnnoPage> {
  private static final Logger logger = LogManager.getLogger(MigrationProgressListener.class);

  private final long loggingInterval;
  private final MigrationJobMetadata jobMetadata;
  private final MigrationRepository repository;
  private final long totalDocs;

  private final AtomicLong nextLoggingThreshold;

  private final Object lock = new Object();

  public MigrationProgressListener(
      MigrationAppSettings appSettings,
      MigrationJobMetadata jobMetadata,
      MigrationRepository repository) {
    this.totalDocs = appSettings.getTotalCount();
    this.jobMetadata = jobMetadata;
    this.repository = repository;
    this.loggingInterval = appSettings.getLoggingInterval();
    this.nextLoggingThreshold = new AtomicLong(this.loggingInterval);
  }

  @Override
  public void onReadError(Exception ex) {
    logger.warn("Error during read", ex);
  }

  @Override
  public void onProcessError(AnnoPage item, Exception e) {
    logger.warn("Error during processing. {}", item, e);
  }

  @Override
  public void afterWrite(List<? extends AnnoPage> item) {
    long writeCount = jobMetadata.addProcessed(item.size());

    if (writeCount > nextLoggingThreshold.get()) {
      synchronized (lock) {
        // prevent multiple threads from saving redundant data to db. Acquiring lock is cheaper than
        // db request
        if (writeCount > nextLoggingThreshold.get()) {
          nextLoggingThreshold.set(nextLoggingThreshold.get() + loggingInterval);
          repository.save(jobMetadata);
          // potential race condition here, as jobMetadata.processedCount could have been changed
          logger.info("Saved job metadata checkpoint: {}", jobMetadata);
          if (totalDocs > 0 && logger.isInfoEnabled()) {
            logger.info(
                "{}% of {} records migrated", String.format("%.2f",(writeCount / (double) totalDocs) * 100), totalDocs);
          }
        }
      }
    }
  }

  @Override
  public void onWriteError(Exception ex, List<? extends AnnoPage> annoPages) {
    logger.warn("Error during write. ObjectIds={}", getAnnoPageObjectIds(annoPages), ex);
  }
}
