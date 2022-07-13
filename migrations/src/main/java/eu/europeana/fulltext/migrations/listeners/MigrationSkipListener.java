package eu.europeana.fulltext.migrations.listeners;

import eu.europeana.fulltext.entity.AnnoPage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Component
public class MigrationSkipListener implements SkipListener<AnnoPage, AnnoPage> {
  private static final Logger logger = LogManager.getLogger(MigrationSkipListener.class);

  @Override
  public void onSkipInRead(Throwable throwable) {
    logger.warn("Item skipped during read", throwable);
  }

  @Override
  public void onSkipInWrite(AnnoPage annoPage, Throwable throwable) {
    logger.warn("Item skipped during write. AnnoPage objectId={}", annoPage.getDbId(), throwable);
  }

  @Override
  public void onSkipInProcess(AnnoPage annoPage, Throwable throwable) {
    logger.warn(
        "Item skipped during processing. AnnoPage objectId={}", annoPage.getDbId(), throwable);
  }
}
