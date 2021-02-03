package eu.europeana.fulltext.api.service.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/**
 * Error that is thrown when there is an error during serialization
 * @author Lúthien
 * Created on 27-02-2018
 */
public class SerializationException extends EuropeanaApiException {

    private static final long serialVersionUID = 939459193481064040L;

    public SerializationException(String msg, Throwable t) {
        super(msg, t);
    }

    //@Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    //@Override
    public boolean doExposeMessage() {
        // Serialization exception message not exposed to end users.
        // Requests with debug query param can still view exception stacktrace.
        return false;
    }
}
