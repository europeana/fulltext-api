package eu.europeana.fulltext.annosync.batch.writer;

import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.bulk.BulkWriteResult;
import eu.europeana.fulltext.annosync.batch.AnnoSyncStats;
import eu.europeana.fulltext.annosync.service.AnnoSyncService;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.exception.MongoConnnectionException;
import java.util.List;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnoPageUpsertWriter implements ItemWriter<AnnoPage> {

  private final AnnoSyncService service;
  private final AnnoSyncStats statsCounter;

  public AnnoPageUpsertWriter(AnnoSyncService annotationService, AnnoSyncStats statsCounter) {
    this.service = annotationService;
    this.statsCounter = statsCounter;
  }

  @Override
  public void write(@NonNull List<? extends AnnoPage> annoPages) throws Exception {
    try {
      BulkWriteResult writeResult = service.upsertAnnoPage(annoPages);

      for (int i = 0; i < writeResult.getUpserts().size(); i++) {
        statsCounter.addNew();
      }

      for (int i = 0; i < writeResult.getModifiedCount(); i++) {
        statsCounter.addUpdated();
      }
    } catch (MongoException e) {
      if (e instanceof MongoSocketException || e instanceof MongoTimeoutException) {
        throw new MongoConnnectionException("Error while connecting to Mongo -"  +e.getMessage());
      }
    }
  }
}
