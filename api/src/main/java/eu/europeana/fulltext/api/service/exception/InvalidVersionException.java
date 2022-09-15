package eu.europeana.fulltext.api.service.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidVersionException extends EuropeanaApiException {

    private static final long serialVersionUID = 2048581559311721229L;

    public InvalidVersionException(String msg) {
        super(msg);
    }

    @Override
    public boolean doLog() {
        return false;
    }

    //@Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
