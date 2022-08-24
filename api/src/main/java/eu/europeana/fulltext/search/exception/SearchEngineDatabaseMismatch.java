package eu.europeana.fulltext.search.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * When Solr returns particular targetIds and we can't find those back in Mongo, then Solr and Mongo are not in sync
 * or there are encoding issues which we missed. To spot those quickly, we'll return a 500 response
 *
 * @author Patrick Ehlert
 * Created on 24 Aug 2022
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SearchEngineDatabaseMismatch extends EuropeanaApiException {

    private static final long serialVersionUID = 6788432801227803308L;

    public SearchEngineDatabaseMismatch() {
        super("Mismatch between search engine and database. Unable to find record and/or targetIds in database");
    }

    @Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
