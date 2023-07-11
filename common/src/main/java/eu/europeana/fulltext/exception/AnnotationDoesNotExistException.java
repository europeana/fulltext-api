package eu.europeana.fulltext.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Error that is thrown when the id of a record is missing or no record with a specified id exists (needs work)
 * @author Lúthien
 * Created on 27-02-2018
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class AnnotationDoesNotExistException extends EuropeanaApiException {

    private static final long serialVersionUID = 2048581559311721229L;

    public AnnotationDoesNotExistException(String id) {
        super("Annotation with id " + id + " does not exist");
    }

    @Override
    public boolean doLog() {
        return false;
    }

    //@Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
