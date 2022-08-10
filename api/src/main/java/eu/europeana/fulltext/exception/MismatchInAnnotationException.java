package eu.europeana.fulltext.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when converting EDMFulltextPackage to Annotations / AnnoPage.
 */
public class MismatchInAnnotationException extends EuropeanaApiException {

  public MismatchInAnnotationException(String msg) {
    super(msg);
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.BAD_REQUEST;
  }
}
