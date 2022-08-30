package eu.europeana.fulltext.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;

public class SolrServiceException extends EuropeanaApiException {

  private static final long serialVersionUID = -167560566275881316L;

  public SolrServiceException(String message, Throwable th) {
    super(message, th);
  }

  public SolrServiceException(String message) {
    super(message);
  }

  @Override
  public boolean doLog() {
    return true;
  }

  @Override
  public boolean doLogStacktrace() {
    return true;
  }

  @Override
  public boolean doExposeMessage() {
    return true;
  }
}
