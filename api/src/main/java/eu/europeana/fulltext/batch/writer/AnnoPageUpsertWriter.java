package eu.europeana.fulltext.batch.writer;

import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.bulk.BulkWriteResult;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.batch.AnnoSyncStats;
import eu.europeana.fulltext.entity.AnnoPage;
import java.util.List;

import eu.europeana.fulltext.exception.MongoConnnectionException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnoPageUpsertWriter implements ItemWriter<AnnoPage> {

  private final FTService ftService;
  private final AnnoSyncStats statsCounter;

  public AnnoPageUpsertWriter(FTService annotationService, AnnoSyncStats statsCounter) {
    this.ftService = annotationService;
    this.statsCounter = statsCounter;
  }

  @Override
  public void write(@NonNull List<? extends AnnoPage> annoPages) throws Exception {
    try {
      BulkWriteResult writeResult = ftService.upsertAnnoPage(annoPages);

      for (int i = 0; i < writeResult.getUpserts().size(); i++) {
        statsCounter.addNew();
      }

      for (int i = 0; i < writeResult.getModifiedCount(); i++) {
        statsCounter.addUpdated();
      }
    } catch (MongoException e) {
      if (e instanceof MongoSocketException | e instanceof MongoTimeoutException |
              e instanceof MongoSocketOpenException | e instanceof MongoTimeoutException) {
        throw new MongoConnnectionException("Error while connecting to Mongo -"  +e.getMessage());
      }
    }
  }
}
