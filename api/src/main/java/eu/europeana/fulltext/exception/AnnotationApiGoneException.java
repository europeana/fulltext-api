package eu.europeana.fulltext.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/** Exception thrown when an annotation has been deleted in Annotations API */
public class AnnotationApiGoneException extends EuropeanaApiException {

  public AnnotationApiGoneException(String msg) {
    super(msg);
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.GONE;
  }

  @Override
  public boolean doLogStacktrace() {
    return false;
  }
}
