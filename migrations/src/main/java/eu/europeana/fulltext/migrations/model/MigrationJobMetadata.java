package eu.europeana.fulltext.migrations.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.bson.types.ObjectId;

@Entity("MigrationJobMetadata")
public class MigrationJobMetadata {

  @Id private ObjectId dbId;
  private final AtomicReference<ObjectId> lastAnnoPageIdRef = new AtomicReference<>();

  private final AtomicLong processedCount;

  public MigrationJobMetadata(ObjectId lastAnnoPageId, AtomicLong processedCount) {
    lastAnnoPageIdRef.set(lastAnnoPageId);
    this.processedCount = processedCount;
  }

  public ObjectId getLastAnnoPageId() {
    return lastAnnoPageIdRef.get();
  }

  public void setLastAnnoPageId(ObjectId lastAnnoPageId) {
    lastAnnoPageIdRef.set(lastAnnoPageId);
  }

  public AtomicLong getProcessedCount() {
    return processedCount;
  }

  public long addProcessed(long processedCount) {
    return this.processedCount.addAndGet(processedCount);
  }

  @Override
  public String toString() {
    return "MigrationJobMetadata{"
        + "lastAnnoPageId="
        + lastAnnoPageIdRef.get()
        + ", processedCount="
        + processedCount.get()
        + '}';
  }
}
