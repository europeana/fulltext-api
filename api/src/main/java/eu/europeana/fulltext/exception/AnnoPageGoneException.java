package eu.europeana.fulltext.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested AnnoPage has been deprecated
 */
public class AnnoPageGoneException extends EuropeanaApiException {

  public AnnoPageGoneException(String id) {
    super("Annotation Page with id " + id + " has been deprecated");
  }

  public AnnoPageGoneException(String id, String language) {
    super("Annotation Page with id " + id + " has been deprecated for language " + language);
  }

  public AnnoPageGoneException(String id, String language, String msg) {
    super("Annotation Page with id " + id + " has been deprecated for language " + language + ". " +msg);
  }

  @Override
  public boolean doLog() {
    return false;
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.GONE;
  }
}
