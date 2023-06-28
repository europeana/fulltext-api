package eu.europeana.fulltext.annosync.batch.writer;

import eu.europeana.fulltext.annosync.batch.AnnoSyncStats;
import eu.europeana.fulltext.annosync.service.AnnoSyncService;

import java.util.ArrayList;
import java.util.List;

import eu.europeana.fulltext.util.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnoPageDeprecationWriter implements ItemWriter<String> {

  private final AnnoSyncService annoSyncService;
  private final AnnoSyncStats statsCounter;

  public AnnoPageDeprecationWriter(AnnoSyncService annoSyncService,
      AnnoSyncStats statsCounter) {
    this.annoSyncService = annoSyncService;
    this.statsCounter = statsCounter;
  }

  @Override
  public void write(@NonNull List<? extends String> deletedAnnotations) throws Exception {
    long deletedCount = annoSyncService.deprecateAnnoPagesWithSources(getSourceIdFromAnnotations(deletedAnnotations));
    for (int i = 0; i < deletedCount; i++) {
      statsCounter.addDeprecated();
    }
  }

  /**
   * Forms a complete annotation item url to search in Fulltext DB as source values
   * @param deletedAnnotations
   * @return
   */
  private List<String> getSourceIdFromAnnotations(List<? extends String> deletedAnnotations) {
    List<String> sources = new ArrayList<>();
    deletedAnnotations.stream().forEach(annotation -> {
      if (StringUtils.startsWith(annotation, "http")) {
        sources.add(annotation);
      } else {
        sources.add(GeneralUtils.getAnnotationPageURI( "/" + annotation));
      }
    });
    return sources;
  }
}
