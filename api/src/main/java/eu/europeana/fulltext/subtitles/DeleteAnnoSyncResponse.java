package eu.europeana.fulltext.subtitles;

import eu.europeana.fulltext.entity.AnnoPage;
import org.springframework.lang.Nullable;

public class DeleteAnnoSyncResponse {

  private final String source;
  private final String status;

  private AnnoPage annoPage;

  public DeleteAnnoSyncResponse(String source, String status, @Nullable AnnoPage annoPage) {
    this.source = source;
    this.status = status;

    // copy required props from AnnoPage
    if (annoPage != null) {
      this.annoPage = new AnnoPage();
      this.annoPage.setDsId(annoPage.getDsId());
      this.annoPage.setLcId(annoPage.getLcId());
      this.annoPage.setPgId(annoPage.getPgId());
      this.annoPage.setLang(annoPage.getLang());
    }
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
