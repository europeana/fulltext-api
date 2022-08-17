package eu.europeana.fulltext.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/** Exception thrown when an Annotation does not exist in Annotations API */
public class AnnotationApiNotFoundException extends EuropeanaApiException {

  public AnnotationApiNotFoundException(String msg) {
    super(msg);
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.NOT_FOUND;
  }

  @Override
  public boolean doLogStacktrace() {
    return false;
  }
}
