package eu.europeana.fulltext.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;

/**
 * Exception thrown when there's an issue with Solr documents
 */
public class SolrDocumentException extends EuropeanaApiException {

  public SolrDocumentException(String msg) {
    super(msg);
  }
}
