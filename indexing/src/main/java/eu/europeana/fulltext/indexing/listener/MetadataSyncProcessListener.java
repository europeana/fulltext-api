package eu.europeana.fulltext.indexing.listener;

import eu.europeana.fulltext.indexing.model.IndexingWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.stereotype.Component;

@Component
public class MetadataSyncProcessListener implements
    ItemProcessListener<String, IndexingWrapper> {

  private static final Logger logger = LogManager.getLogger(FulltextIndexingListener.class);

  @Override
  public void beforeProcess(String item) {
    // do nothing
  }

  @Override
  public void afterProcess(String item, IndexingWrapper result) {
// do nothing
  }

  @Override
  public void onProcessError(String item, Exception e) {
    logger.warn("Error during processing. {}", item, e);
  }
}
