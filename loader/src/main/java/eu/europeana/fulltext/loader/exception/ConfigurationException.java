package eu.europeana.fulltext.loader.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception that is thrown when there is a problem with the application's configuration
 * @author Patrick Ehlert
 * @@deprecated since 2023
 * <p>
 * Created on 24-08-2018
 */
@Deprecated
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ConfigurationException extends LoaderException {

    private static final long serialVersionUID = 5022468163458616001L;

    public ConfigurationException(String msg) {
        super(msg);
    }

    public ConfigurationException(String msg, Throwable t) {
        super(msg, t);
    }
}
