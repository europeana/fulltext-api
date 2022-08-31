package eu.europeana.fulltext.indexing.listener;

import static eu.europeana.fulltext.indexing.IndexingConstants.getRecordId;

import eu.europeana.fulltext.indexing.model.AnnoPageRecordId;
import eu.europeana.fulltext.indexing.model.IndexingWrapper;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.stereotype.Component;

/**
 * Listener for hooking into read, processing and write operations. Also handles logging of errors
 */
@Component
public class FulltextIndexingListener
    extends ItemListenerSupport<AnnoPageRecordId, IndexingWrapper> {
  private static final Logger logger = LogManager.getLogger(FulltextIndexingListener.class);

  @Override
  public void onReadError(Exception ex) {
    logger.warn("Error during read", ex);
  }

  @Override
  public void onProcessError(AnnoPageRecordId item, Exception e) {
    logger.warn("Error during processing. {}", item, e);
  }

  @Override
  public void onWriteError(Exception ex, List<? extends IndexingWrapper> list) {
    logger.warn("Error during write. ObjectIds={}", getRecordId(list), ex);
  }
}
