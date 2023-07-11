package eu.europeana.fulltext.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an unsupported media type is provided when submitting fulltext documents.
 */
public class MediaTypeNotSupportedException extends EuropeanaApiException {

  /**
   * Initialise a new exception for which there is no root cause
   *
   * @param msg error message
   */
  public MediaTypeNotSupportedException(String msg) {
    super(msg);
  }

  /**
   * Initialise a new exception for which there is no root cause
   *
   * @param msg error message
   * @param errorCode error code
   */
  public MediaTypeNotSupportedException(String msg, String errorCode) {
    super(msg, errorCode);
  }

  /**
   * We don't want to log the stack trace for this exception
   *
   * @return false
   */
  @Override
  public boolean doLogStacktrace() {
    return false;
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
  }
}
