package eu.europeana.fulltext.migrations;

import eu.europeana.fulltext.migrations.config.MigrationAppSettings;
import eu.europeana.fulltext.migrations.repository.MigrationRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;

public class MigrationProgressLogger implements ChunkListener {
  private static final Logger logger = LogManager.getLogger(MigrationProgressLogger.class);

  private final int loggingInterval;
  private final MigrationJobMetadata jobMetadata;
  private final MigrationRepository repository;

  public MigrationProgressLogger(
      MigrationAppSettings appSettings,
      MigrationJobMetadata jobMetadata,
      MigrationRepository repository) {
    this.loggingInterval = appSettings.getLoggingInterval();
    this.jobMetadata = jobMetadata;
    this.repository = repository;
  }

  @Override
  public void beforeChunk(ChunkContext chunkContext) {
    // do nothing here
  }

  @Override
  public void afterChunk(ChunkContext chunkContext) {
    int count = chunkContext.getStepContext().getStepExecution().getReadCount();

    // If the number of records processed so far is a multiple of the logging interval then output a
    // log message.
    if (count > 0 && count % loggingInterval == 0) {
      jobMetadata.setProcessedCount(count);
      repository.save(jobMetadata);
      logger.info("Saved job metadata checkpoint: {}", jobMetadata);
    }
  }

  @Override
  public void afterChunkError(ChunkContext chunkContext) {
    // do nothing here
  }
}
