package eu.europeana.fulltext.subtitles;

import eu.europeana.fulltext.entity.AnnoPage;

public class DeleteAnnoSyncResponse {

  private final String source;
  private final String status;

  private AnnoPage annoPage;

  public DeleteAnnoSyncResponse(String source, String status, AnnoPage annoPage) {
    this.source = source;
    this.status = status;
    this.annoPage = annoPage;
  }

  public String getStatus() {
    return status;
  }

  public AnnoPage getAnnoPage() {
    return annoPage;
  }

  public void setAnnoPage(AnnoPage annoPage) {
    this.annoPage = annoPage;
  }

  public enum Status {
    DELETED("deleted"),
    NOOP("no-op");

    private String value;

    Status(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
