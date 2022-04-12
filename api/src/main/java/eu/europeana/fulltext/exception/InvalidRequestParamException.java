package eu.europeana.fulltext.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRequestParamException extends EuropeanaApiException {

    private static final long serialVersionUID = 2048581559311721229L;

    public InvalidRequestParamException(String param, String paramValue) {
        super("Invalid request parameter value. " + param + ":" + paramValue);
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
