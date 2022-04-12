package eu.europeana.fulltext.subtitles.edm;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.util.GeneralUtils;
import java.util.ArrayList;
import java.util.List;

public class EdmAnnotation {

  private final String annoId;
  private final EdmReference textReference;
  private final List<EdmTimeBoundary> targets = new ArrayList<>(1);
  private final AnnotationType type;
  private final String lang;
  private final Float confidence;

  public EdmAnnotation(
      String annoId,
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
    this.annoId = (annoId != null ? annoId : GeneralUtils.generateHash(this));
  }

  public String getAnnoId() {
    return annoId;
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
