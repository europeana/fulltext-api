package eu.europeana.fulltext.search.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.fulltext.search.model.query.EuropeanaId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception throw when the requested issue does not exist
 *
 * @author Patrick Ehlert
 * Created on 28 May 2020
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RecordDoesNotExistException extends EuropeanaApiException {

    private static final long serialVersionUID = -2506967519765835153L;

    public RecordDoesNotExistException(EuropeanaId europeanaId) {
        super("No record with id '" + europeanaId + "' found");
    }

    //@Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
