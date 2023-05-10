package eu.europeana.fulltext.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

public class XmlParsingException extends EuropeanaApiException {
    /**
     * Initialise a new exception for which there is no root cause
     *
     * @param msg error message
     */
    public XmlParsingException(String msg) {
        super(msg);
    }

    /**
     * Initialise a new exception for which there is no root cause
     *
     * @param msg error message
     * @param errorCode error code
     */
    public XmlParsingException(String msg, String errorCode) {
        super(msg, errorCode);
    }

    /**
     * We don't want to log the stack trace for this exception
     *
     * @return false
     */
    @Override
    public boolean doLogStacktrace() {
        return false;
    }

    @Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
