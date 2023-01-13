package eu.europeana.fulltext.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when requests to Annotation API fail for any reason. Use {@link
 * AnnotationApiNotFoundException} If Annotation API handles request, but returns 404.
 */
public class AnnotationApiRequestException extends EuropeanaApiException {

  public AnnotationApiRequestException(String msg) {
    super(msg);
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.BAD_GATEWAY;
  }
}
