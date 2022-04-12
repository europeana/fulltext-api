package eu.europeana.fulltext.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;

/** Exception thrown when constructing or executing database queries */
public class DatabaseQueryException extends EuropeanaApiException {

  public DatabaseQueryException(String msg) {
    super(msg);
  }
}
