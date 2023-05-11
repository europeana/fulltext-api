package eu.europeana.fulltext.annosync.batch.reader;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.item.data.AbstractPaginatedDataItemReader;
import eu.europeana.fulltext.service.AnnotationApiRestService;
import eu.europeana.fulltext.subtitles.external.AnnotationItem;

public class AnnotationItemReader extends AbstractPaginatedDataItemReader<AnnotationItem> {

  private static final Logger logger = LogManager.getLogger(AnnotationItemReader.class);

  private final AnnotationApiRestService annotationsRestService;
  private final Instant from;
  private final Instant to;

  public AnnotationItemReader(
      AnnotationApiRestService annotationsRestService, int pageSize, Instant from, Instant to) {
    setPageSize(pageSize);
    this.annotationsRestService = annotationsRestService;
    this.from = from;
    this.to = to;
    // Non-restartable, as we expect this to run in multithreaded steps.
    // see: https://stackoverflow.com/a/20002493
    setSaveState(false);
  }

  String getClassName() {
    return AnnotationItemReader.class.getSimpleName();
  }

  @Override
  protected Iterator<AnnotationItem> doPageRead() {
    // page is incremented in parent class every time this method is invoked
    List<AnnotationItem> searchResponse =
        annotationsRestService.getAnnotations(page, pageSize, from, to);

    if (searchResponse == null || searchResponse.isEmpty()) {
      logger.info("No results found in page:{} , pageSize:{}, from: {}, to: {} ", page, pageSize, from, to);
      return null;
    }
    
    logger.info("Fetched Annotations ids - {} ", searchResponse.size());

    return searchResponse.iterator();
  }

  @Override
  protected void doOpen() throws Exception {
    super.doOpen();
    setName(getClassName());
  }
}
