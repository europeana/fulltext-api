package eu.europeana.fulltext.subtitles;

import eu.europeana.fulltext.entity.TranslationAnnoPage;

public class DeleteAnnoSyncResponse {

  private final String source;
  private final String status;

  private TranslationAnnoPage annoPage;

  public DeleteAnnoSyncResponse(String source, String status, TranslationAnnoPage annoPage) {
    this.source = source;
    this.status = status;
    this.annoPage = annoPage;
  }

  public String getStatus() {
    return status;
  }

  public TranslationAnnoPage getAnnoPage() {
    return annoPage;
  }

  public void setAnnoPage(TranslationAnnoPage annoPage) {
    this.annoPage = annoPage;
  }

  public static enum Status {
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
