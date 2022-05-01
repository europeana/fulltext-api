package eu.europeana.fulltext.batch;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Thread-safe counter track of new, updated and deleted AnnoPages when syncing Annotations.
 *
 * Before use, call {@link AnnoSyncStats#reset()}
 */
@Component
public class AnnoSyncStats {
  private final AtomicInteger newAnnotations = new AtomicInteger();
  private final AtomicInteger updatedAnnotations = new AtomicInteger();
  private final AtomicInteger deletedAnnotations = new AtomicInteger();

  public void reset() {
    newAnnotations.set(0);
    updatedAnnotations.set(0);
    deletedAnnotations.set(0);
  }

  public void addNew() {
    newAnnotations.incrementAndGet();
  }

  public void addUpdated() {
    updatedAnnotations.incrementAndGet();
  }

  public void addDeleted() {
    deletedAnnotations.incrementAndGet();
  }

  public int getNew() {
    return newAnnotations.get();
  }

  public int getUpdated() {
    return updatedAnnotations.get();
  }

  public int getDeleted() {
    return deletedAnnotations.get();
  }
}
