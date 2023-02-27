package eu.europeana.fulltext.annosync.batch;

import eu.europeana.fulltext.annosync.batch.model.AnnoSyncJobMetadata;
import eu.europeana.fulltext.annosync.batch.repository.AnnoSyncJobMetadataRepo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class AnnoSyncMetadataUpdaterTasklet implements Tasklet {

  private final AnnoSyncJobMetadataRepo repository;
  private final AnnoSyncJobMetadata metadata;
  private static final Logger logger = LogManager.getLogger(AnnoSyncMetadataUpdaterTasklet.class);

  public AnnoSyncMetadataUpdaterTasklet(
      AnnoSyncJobMetadataRepo repository, AnnoSyncJobMetadata metadata) {
    this.repository = repository;
    this.metadata = metadata;
  }

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext)
      throws Exception {
    repository.save(metadata);
    logger.info("Saved annoSync metadata {}", metadata);

    return RepeatStatus.FINISHED;
  }
}
