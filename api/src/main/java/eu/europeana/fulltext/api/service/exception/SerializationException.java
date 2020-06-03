package eu.europeana.fulltext.api.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Error that is thrown when there is an error during serialization
 * @author Lúthien
 * Created on 27-02-2018
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SerializationException extends FTException {

    private static final long serialVersionUID = 939459193481064040L;

    public SerializationException(String msg, Throwable t) {
        super(msg, t);
    }
}
