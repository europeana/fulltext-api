package eu.europeana.fulltext.migrations;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import java.util.concurrent.atomic.AtomicLong;
import org.bson.types.ObjectId;

@Entity("MigrationJobMetadata")
public class MigrationJobMetadata {

  @Id private ObjectId dbId;

  public MigrationJobMetadata(ObjectId lastAnnoPageId,
      AtomicLong processedCount) {
    this.lastAnnoPageId = lastAnnoPageId;
    this.processedCount = processedCount;
  }

  private volatile ObjectId lastAnnoPageId;
  private final AtomicLong processedCount;

  public ObjectId getLastAnnoPageId() {
    return lastAnnoPageId;
  }

  public void setLastAnnoPageId(ObjectId lastAnnoPageId) {
    this.lastAnnoPageId = lastAnnoPageId;
  }

  public AtomicLong getProcessedCount() {
    return processedCount;
  }

  public void setProcessedCount(long processedCount) {
    this.processedCount.set(processedCount);
  }

  @Override
  public String toString() {
    return "MigrationJobMetadata{"
        + "lastAnnoPageId="
        + lastAnnoPageId
        + ", processedCount="
        + processedCount.get()
        + '}';
  }
}
