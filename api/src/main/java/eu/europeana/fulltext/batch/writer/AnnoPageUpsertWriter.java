package eu.europeana.fulltext.batch.writer;

import com.mongodb.bulk.BulkWriteResult;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.batch.AnnoSyncStats;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import java.util.List;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnoPageUpsertWriter implements ItemWriter<TranslationAnnoPage> {

  private final FTService ftService;
  private final AnnoSyncStats statsCounter;

  public AnnoPageUpsertWriter(FTService annotationService, AnnoSyncStats statsCounter) {
    this.ftService = annotationService;
    this.statsCounter = statsCounter;
  }

  @Override
  public void write(@NonNull List<? extends TranslationAnnoPage> annoPages) throws Exception {
    BulkWriteResult writeResult = ftService.upsertAnnoPage(annoPages);

    for (int i = 0; i < writeResult.getUpserts().size(); i++) {
      statsCounter.addNew();
    }

    for (int i = 0; i < writeResult.getModifiedCount(); i++) {
      statsCounter.addUpdated();
    }

  }
}
