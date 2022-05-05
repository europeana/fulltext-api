package eu.europeana.fulltext.batch;

import java.time.Duration;

public class BatchUtils {

  private BatchUtils() {
    // hide implicit public constructor
  }

  public static final String ANNO_SYNC_JOB = "synchroniseAnnoJob";


  /**
   * Generates a human-readable string for a duration
   * @param duration
   * @return string containing duration in easy readable format
   */
  public static String getDurationText(Duration duration) {
    String result;

    if (duration.toDaysPart() >= 1) {
      result = String.format("%d days, %d hours and %d minutes", duration.toDaysPart(), duration.toHoursPart(), duration.toMinutesPart());
    } else if (duration.toHoursPart() >= 1) {
      result = String.format("%d hours and %d minutes", duration.toHoursPart(), duration.toMinutesPart());
    } else if (duration.toMinutesPart() >= 1){
      result = String.format("%d minutes and %d seconds", duration.toMinutesPart(), duration.toSecondsPart());
    } else {
      result = String.format("%d.%d seconds", duration.toSecondsPart(), duration.toMillisPart());
    }
    return result;
  }
}
