package eu.europeana.fulltext.annosync.batch.listener;

import static eu.europeana.fulltext.util.GeneralUtils.getAnnoPageToString;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.subtitles.external.AnnotationItem;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnoSyncUpdateListener extends ItemListenerSupport<AnnotationItem, AnnoPage> {
  private static final Logger logger = LogManager.getLogger(AnnoSyncUpdateListener.class);

  @Override
  public void onReadError(@NonNull Exception e) {
    // No item linked to error, so we just log a warning
    logger.error("onReadError", e);
  }

  @Override
  public void onProcessError(@NonNull AnnotationItem item, @NonNull Exception e) {
    // just log warning for now
    logger.error(
        "Error processing AnnotationItem id={}; recordId={}",
        item.getId(),
        item.getTarget().getScope(),
        e);
  }

  @Override
  public void onWriteError(@NonNull Exception ex, @NonNull List<? extends AnnoPage> annoPages) {
    logger.error("Error saving AnnoPages {}, ", getAnnoPageToString(annoPages), ex);
  }
}
