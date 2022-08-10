package eu.europeana.fulltext.edm;

public class EdmFullTextResource implements EdmReference {

  private final String fullTextResourceURI;
  private String value;
  private final String lang;
  private final String rights;
  private final String recordURI;

  public EdmFullTextResource(
      String fullTextResourceURI, String value, String lang, String rights, String recordURI) {
    this.fullTextResourceURI = fullTextResourceURI;
    this.value = value;
    this.lang = lang;
    this.rights = rights;
    this.recordURI = recordURI;
  }

  public String getFullTextResourceURI() {
    return fullTextResourceURI;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getLang() {
    return lang;
  }

  public String getRights() {
    return rights;
  }

  public String getRecordURI() {
    return recordURI;
  }

  public EdmFullTextResource getResource() {
    return this;
  }

  @Override
  public String getResourceURL() {
    return getFullTextResourceURI();
  }

  @Override
  public String getURL() {
    return getResourceURL();
  }
}
