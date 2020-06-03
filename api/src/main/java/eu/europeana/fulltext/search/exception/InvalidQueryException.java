package eu.europeana.fulltext.search.exception;

import eu.europeana.fulltext.api.service.exception.FTException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception throw when the search request is invalid (e.g. no query or q parameter)
 *
 * @author Patrick Ehlert
 * Created on 28 May 2020
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidQueryException extends FTException {

    public InvalidQueryException(String query) {
        super("Invalid search query:" + query);
    }

}
