package eu.europeana.fulltext.batch.reader;

import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.service.AnnotationApiRestService;
import eu.europeana.fulltext.subtitles.external.AnnotationItem;
import java.time.Instant;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ItemReaderConfig {
  private final AnnotationApiRestService annotationsApiRestService;
  private final FTSettings appSettings;

  public ItemReaderConfig(
      AnnotationApiRestService annotationsApiRestService, FTSettings appSettings) {
    this.annotationsApiRestService = annotationsApiRestService;
    this.appSettings = appSettings;
  }

  public SynchronizedItemStreamReader<AnnotationItem> createAnnotationReader(
      Instant from, Instant to) {
    AnnotationItemReader reader =
        new AnnotationItemReader(
            annotationsApiRestService, appSettings.getAnnotationItemsPageSize(), from, to);
    return threadSafeReader(reader);
  }

  public SynchronizedItemStreamReader<String> createDeletedAnnotationReader(
      Instant from, Instant to) {
    DeletedAnnotationsReader reader =
        new DeletedAnnotationsReader(
            annotationsApiRestService, appSettings.getAnnotationItemsPageSize(), from, to);
    return threadSafeReader(reader);
  }

  /** Makes ItemReader thread-safe */
  private <T> SynchronizedItemStreamReader<T> threadSafeReader(ItemStreamReader<T> reader) {
    final SynchronizedItemStreamReader<T> synchronizedItemStreamReader =
        new SynchronizedItemStreamReader<>();
    synchronizedItemStreamReader.setDelegate(reader);
    return synchronizedItemStreamReader;
  }
}
