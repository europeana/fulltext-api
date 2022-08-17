package eu.europeana.fulltext.edm;

import eu.europeana.fulltext.AnnotationType;

import java.util.ArrayList;
import java.util.List;

public class EdmAnnotation {

  private final EdmReference textReference;
  private final List<EdmTimeBoundary> targets = new ArrayList<>(1);
  private final AnnotationType type;
  private final String lang;
  private final Float confidence;

  public EdmAnnotation(
      EdmReference textReference,
      EdmTimeBoundary target,
      AnnotationType type,
      String lang,
      Float confidence) {
    this.textReference = textReference;
    if (target != null) {
      targets.add(target);
    }
    this.type = type;
    this.lang = lang;
    this.confidence = confidence;
  }

  public EdmReference getTextReference() {
    return textReference;
  }

  public List<EdmTimeBoundary> getTargets() {
    return targets;
  }

  public AnnotationType getType() {
    return type;
  }

  public String getLang() {
    return lang;
  }

  public Float getConfidence() {
    return confidence;
  }

  public boolean hasTargets() {
    return !targets.isEmpty();
  }
}
