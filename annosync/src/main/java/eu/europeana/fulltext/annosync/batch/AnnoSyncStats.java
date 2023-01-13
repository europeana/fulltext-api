package eu.europeana.fulltext.annosync.batch;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Thread-safe counter to track new, updated and deprecated AnnoPages when syncing Annotations.
 *
 * <p>Before use, call {@link AnnoSyncStats#reset()}
 */
@Component
public class AnnoSyncStats {
  private final AtomicInteger newAnnotations = new AtomicInteger();
  private final AtomicInteger updatedAnnotations = new AtomicInteger();
  private final AtomicInteger deprecatedAnnotations = new AtomicInteger();

  private Instant startTime;
  private Duration elapsedTime;

  public void reset() {
    newAnnotations.set(0);
    updatedAnnotations.set(0);
    deprecatedAnnotations.set(0);

    startTime = null;
    elapsedTime = Duration.ZERO;
  }

  public void setStartTime(Instant startTime) {
    this.startTime = startTime;
  }

  public void setElapsedTime(Duration elapsedTime) {
    this.elapsedTime = elapsedTime;
  }

  public void addNew() {
    newAnnotations.incrementAndGet();
  }

  public void addUpdated() {
    updatedAnnotations.incrementAndGet();
  }

  public void addDeprecated() {
    deprecatedAnnotations.incrementAndGet();
  }

  public int getNew() {
    return newAnnotations.get();
  }

  public int getUpdated() {
    return updatedAnnotations.get();
  }

  public int getDeprecated() {
    return deprecatedAnnotations.get();
  }

  public Duration getElapsedTime() {
    return elapsedTime;
  }

  public Instant getStartTime() {
    return startTime;
  }
}
