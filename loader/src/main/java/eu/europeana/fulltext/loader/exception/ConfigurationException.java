package eu.europeana.fulltext.loader.exception;

/**
 * Exception that is thrown when there is a problem with the application's configuration
 * @author Patrick Ehlert
 * <p>
 * Created on 24-08-2018
 */
public class ConfigurationException extends LoaderException {

    public ConfigurationException(String msg) {
        super(msg);
    }

    public ConfigurationException(String msg, Throwable t) {
        super(msg, t);
    }
}
