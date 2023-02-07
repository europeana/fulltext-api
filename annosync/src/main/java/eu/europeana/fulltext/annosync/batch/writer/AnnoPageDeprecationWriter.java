package eu.europeana.fulltext.annosync.batch.writer;

import eu.europeana.fulltext.annosync.batch.AnnoSyncStats;
import eu.europeana.fulltext.annosync.service.AnnoSyncService;
import java.util.List;
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
  public void write(@NonNull List<? extends String> annoPages) throws Exception {
    long deletedCount = annoSyncService.deprecateAnnoPagesWithSources(annoPages);

    for (int i = 0; i < deletedCount; i++) {
      statsCounter.addDeprecated();
    }
  }
}
