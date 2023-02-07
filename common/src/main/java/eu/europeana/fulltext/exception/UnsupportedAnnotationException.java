package eu.europeana.fulltext.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/** Exception thrown trying to sync an unsupported annotation */
public class UnsupportedAnnotationException extends EuropeanaApiException {

  public UnsupportedAnnotationException(String msg) {
    super(msg);
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.UNPROCESSABLE_ENTITY;
  }

  @Override
  public boolean doLogStacktrace() {
    return false;
  }
}
