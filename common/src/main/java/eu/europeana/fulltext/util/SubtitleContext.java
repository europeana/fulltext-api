package eu.europeana.fulltext.util;

import eu.europeana.fulltext.edm.EdmTextBoundary;

/** Class handles the values of the subtitle and generates the Fulltext resource value */
public class SubtitleContext {
  private final StringBuilder textValue = new StringBuilder();
  private String fulltextURI = null;

  public void start(String fulltextURl) {
    fulltextURI = fulltextURl;
  }

  public EdmTextBoundary newItem(String str) {
    int s = textValue.length();
    textValue.append(str);
    int e = textValue.length();
    return new EdmTextBoundary(fulltextURI, s, e);
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
