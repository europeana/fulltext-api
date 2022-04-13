package eu.europeana.fulltext.batch.reader;

import eu.europeana.fulltext.api.service.AnnotationApiRestService;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import org.springframework.batch.item.data.AbstractPaginatedDataItemReader;

public class DeletedAnnotationsReader extends AbstractPaginatedDataItemReader<String> {

  private final AnnotationApiRestService annotationsRestService;
  private final Instant from;
  private final Instant to;

  public DeletedAnnotationsReader(
      AnnotationApiRestService annotationsRestService, int pageSize, Instant from, Instant to) {
    setPageSize(pageSize);
    this.annotationsRestService = annotationsRestService;
    this.from = from;
    this.to = to;
    // Non-restartable, as we expect this to run in multi-threaded steps.
    // see: https://stackoverflow.com/a/20002493
    setSaveState(false);
  }

  @Override
  protected Iterator<String> doPageRead() {
    // pageSize is incremented in parent class every time this method is invoked
    List<String> deletedAnnotations =
        annotationsRestService.getDeletedAnnotations(page, pageSize, from, to);

    if (deletedAnnotations == null || deletedAnnotations.isEmpty()) {
      return null;
    }

    return deletedAnnotations.iterator();
  }
}
