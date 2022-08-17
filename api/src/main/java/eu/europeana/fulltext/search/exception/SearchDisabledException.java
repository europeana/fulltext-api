package eu.europeana.fulltext.search.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a search request comes in, but the app has Solr disabled:
 * spring.data.solr.repositories.enabled = false
 */
public class SearchDisabledException extends EuropeanaApiException {

  public SearchDisabledException() {
    super("Request not handled as search is currently disabled");
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.NOT_IMPLEMENTED;
  }
}
