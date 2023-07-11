package eu.europeana.fulltext.annosync.batch.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import java.time.Instant;
import org.bson.types.ObjectId;

@Entity("AnnoSyncJobMetadata")
public class AnnoSyncJobMetadata {

  @Id private ObjectId dbId;
  private Instant lastSuccessfulStartTime;

  public AnnoSyncJobMetadata() {
    // no-arg constructor
  }

  public AnnoSyncJobMetadata(Instant lastSuccessfulStartTime) {
    this.lastSuccessfulStartTime = lastSuccessfulStartTime;
  }

  public Instant getLastSuccessfulStartTime() {
    return lastSuccessfulStartTime;
  }

  public void setLastSuccessfulStartTime(Instant lastSuccessfulStartTime) {
    this.lastSuccessfulStartTime = lastSuccessfulStartTime;
  }

  @Override
  public String toString() {
    return "AnnoSyncJobMetadata{"
        + "lastSuccessfulStartTime="
        + lastSuccessfulStartTime
        + '}';
  }
}
