package eu.europeana.fulltext.search.exception;

import eu.europeana.fulltext.api.service.exception.FTException;
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
public class RecordDoesNotExistException extends FTException {

    public RecordDoesNotExistException(EuropeanaId europeanaId) {
        super("No record with id '" + europeanaId + "' found");
    }

}
