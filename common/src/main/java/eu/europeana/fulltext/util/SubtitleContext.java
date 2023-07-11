package eu.europeana.fulltext.util;

import eu.europeana.edm.text.FullTextResource;
import eu.europeana.edm.text.TextBoundary;

/** Class handles the values of the subtitle and generates the Fulltext resource value */
public class SubtitleContext {
  private final StringBuilder textValue = new StringBuilder();
  private String fulltextURI = null;

  public void start(String fulltextURl) {
    fulltextURI = fulltextURl;
  }

  public TextBoundary newItem(String str, FullTextResource resource) {
    int s = textValue.length();
    textValue.append(str);
    int e = textValue.length();
    return new TextBoundary(resource, s, e);
  }

  public void separator() {
    textValue.append('\n');
  }

  public String end() {
    try {
      return textValue.toString();
    } finally {
      textValue.setLength(0);
      fulltextURI = null;
    }
  }
}
