package eu.europeana.fulltext.edm;

import java.util.ArrayList;

public class EdmFullTextPackage extends ArrayList<EdmAnnotation> {

  private String baseURI;
  private EdmFullTextResource resource;

  public EdmFullTextPackage(String baseURI, EdmFullTextResource resource) {
    this.resource = resource;
    this.baseURI = baseURI;
  }

  public String getBaseURI() {
    return baseURI;
  }

  public void setBaseURI(String baseURI) {
    this.baseURI = baseURI;
  }

  public EdmFullTextResource getResource() {
    return resource;
  }

  public void setResource(EdmFullTextResource resource) {
    this.resource = resource;
  }

  public boolean isLangOverriden(String lang) {
    if (lang == null) {
      return false;
    }
    return !lang.equals(resource.getLang());
  }
}
