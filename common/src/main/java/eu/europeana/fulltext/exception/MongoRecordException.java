package eu.europeana.fulltext.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;

/**
 * Exception thrown when there's an unexpected issue with Mongo records
 */
public class MongoRecordException extends EuropeanaApiException {

  public MongoRecordException(String msg) {
    super(msg);
  }
}
