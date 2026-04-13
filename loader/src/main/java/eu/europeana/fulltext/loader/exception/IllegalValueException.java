package eu.europeana.fulltext.loader.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a field or attribute value is incorrect
 * @author Patrick Ehlert
 * @deprecated 2023
 * Created on 20-10-2018
 */
@Deprecated
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class IllegalValueException extends LoaderException {

    private static final long serialVersionUID = 4206287459589008850L;

    public IllegalValueException(String msg) {
        super(msg);
    }

    public IllegalValueException(String msg, Throwable t) {
        super(msg, t);
    }
}
