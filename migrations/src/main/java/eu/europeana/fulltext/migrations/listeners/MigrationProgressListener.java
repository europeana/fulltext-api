package eu.europeana.fulltext.migrations.listeners;

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

  private final int loggingInterval;
  private final MigrationJobMetadata jobMetadata;
  private final MigrationRepository repository;

  // helps to prevent duplicate logging from multiple threads
  private volatile long writeCount;

  public MigrationProgressListener(
      MigrationAppSettings appSettings,
      MigrationJobMetadata jobMetadata,
      MigrationRepository repository) {
    this.loggingInterval = appSettings.getLoggingInterval();
    this.jobMetadata = jobMetadata;
    this.repository = repository;
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
   writeCount = jobMetadata.addProcessed(item.size());

    if (writeCount > 0 && writeCount % loggingInterval == 0) {
      repository.save(jobMetadata);
      logger.info("Saved job metadata checkpoint: {}", jobMetadata);
    }
  }

  @Override
  public void onWriteError(Exception ex, List<? extends AnnoPage> annoPages) {
    logger.warn("Error during write. ObjectIds={}", getAnnoPageObjectIds(annoPages), ex);
  }

  private String[] getAnnoPageObjectIds(List<? extends AnnoPage> annoPages) {
    return annoPages.stream().map(a -> a.getDbId().toString()).toArray(String[]::new);
  }
}
