package eu.europeana.fulltext.subtitles;

public class AnnotationPreview {

  private final String source;
  private final String recordId;
  private final String media;
  private final String language;
  private final String rights;
  private final boolean originalLang;
  private final FulltextType fulltextType;
  private String annotationBody;

  private AnnotationPreview(
      String source,
      String recordId,
      String media,
      String language,
      String rights,
      boolean originalLang,
      FulltextType fulltextType,
      String annotationBody) {
    this.source = source;
    this.recordId = recordId;
    this.media = media;
    this.language = language;
    this.rights = rights;
    this.originalLang = originalLang;
    this.fulltextType = fulltextType;
    this.annotationBody = annotationBody;
  }

  public String getRecordId() {
    return recordId;
  }

  public String getMedia() {
    return media;
  }

  public String getLanguage() {
    return language;
  }

  public String getRights() {
    return rights;
  }

  public String getAnnotationBody() {
    return annotationBody;
  }

  public boolean isOriginalLang() {
    return originalLang;
  }

  public FulltextType getFulltextType() {
    return fulltextType;
  }

  public String getSource() {
    return source;
  }

  public static class Builder {
    private String source;
    private String recordId;
    private String media;
    private String language;
    private String rights;
    private boolean originalLang;
    private FulltextType fulltextType;
    private String annotationBody;

    public Builder(String recordId, FulltextType fulltextType, String annotationBody) {
      this.recordId = recordId;
      this.fulltextType = fulltextType;
      this.annotationBody = annotationBody;
    }

    public Builder setMedia(String media) {
      this.media = media;
      return this;
    }

    public Builder setLanguage(String language) {
      this.language = language;
      return this;
    }

    public Builder setRights(String rights) {
      this.rights = rights;
      return this;
    }

    public Builder setOriginalLang(boolean originalLang) {
      this.originalLang = originalLang;
      return this;
    }

    public Builder setFulltextType(FulltextType fulltextType) {
      this.fulltextType = fulltextType;
      return this;
    }

    public Builder setSource(String source) {
      this.source = source;
      return this;
    }

    public AnnotationPreview build() {
      return new AnnotationPreview(
          source, recordId, media, language, rights, originalLang, fulltextType, annotationBody);
    }
  }
}
