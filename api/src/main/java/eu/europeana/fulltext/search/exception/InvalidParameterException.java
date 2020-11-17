package eu.europeana.fulltext.search.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception throw when we detect an invalid request parameter, e.g.
 * <li>no query or q parameter</li>
 * <li>pagSize parameter is bigger than maximum</li>
 *
 * @author Patrick Ehlert
 * Created on 28 May 2020
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidParameterException extends EuropeanaApiException {

    private static final long serialVersionUID = -5555912949147932111L;

    public InvalidParameterException(String error) {
        super("Invalid parameter:" + error);
    }

}
