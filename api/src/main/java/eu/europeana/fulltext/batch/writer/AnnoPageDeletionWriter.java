package eu.europeana.fulltext.batch.writer;

import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.batch.AnnoSyncStats;
import java.util.List;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnoPageDeletionWriter implements ItemWriter<String> {

  private final FTService ftService;
  private final AnnoSyncStats statsCounter;

  public AnnoPageDeletionWriter(FTService ftWriteService,
      AnnoSyncStats statsCounter) {
    this.ftService = ftWriteService;
    this.statsCounter = statsCounter;
  }

  @Override
  public void write(@NonNull List<? extends String> annoPages) throws Exception {
    long deletedCount = ftService.deleteAnnoPagesWithSources(annoPages);

    for (int i = 0; i < deletedCount; i++) {
      statsCounter.addDeleted();
    }
  }
}
