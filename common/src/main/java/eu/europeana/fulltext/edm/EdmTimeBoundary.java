package eu.europeana.fulltext.edm;

import org.apache.commons.lang3.time.DurationFormatUtils;

public class EdmTimeBoundary implements EdmReference {

  private static final String FORMAT = "HH:mm:ss.SSS";

  private final String resourceUrl;
  private int start;
  private int end;

  public EdmTimeBoundary(String resourceUrl) {
    this.resourceUrl = resourceUrl;
  }

  public EdmTimeBoundary(String resourceUrl, int start, int end) {
    this.resourceUrl = resourceUrl;
    this.start = start;
    this.end = end;
  }

  public int getStart() {
    return start;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public int getEnd() {
    return end;
  }

  public void setEnd(int end) {
    this.end = end;
  }

  public String getFragment() {
    return ("#t="
        + DurationFormatUtils.formatDuration(this.start, FORMAT)
        + ","
        + DurationFormatUtils.formatDuration(this.end, FORMAT));
  }

  public boolean isValid() {
    return (this.start >= 0 && this.end >= 0 && this.start < this.end);
  }

  @Override
  public String getResourceURL() {
    return resourceUrl;
  }

  @Override
  public String getURL() {
    return getResourceURL() + getFragment();
  }
}
