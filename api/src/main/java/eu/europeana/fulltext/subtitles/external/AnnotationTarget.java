package eu.europeana.fulltext.subtitles.external;

public class AnnotationTarget {
  private String scope;
  private String source;

  public AnnotationTarget() {}

  public AnnotationTarget(String scope) {
    this.scope = scope;
  }

  public String getScope() {
    return scope;
  }

  public String getSource() {
    return source;
  }
}
