package eu.europeana.fulltext.subtitles.external;

public class AnnotationBody {
  private String value;
  private String edmRights;
  private String format;
  private String language;

  public AnnotationBody() {}

  public AnnotationBody(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public String getEdmRights() {
    return edmRights;
  }

  public String getFormat() {
    return format;
  }

  public String getLanguage() {
    return language;
  }
}
