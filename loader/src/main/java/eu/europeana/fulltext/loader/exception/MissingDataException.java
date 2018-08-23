package eu.europeana.fulltext.loader.exception;

/**
 * Exception that is thrown when required data is missing / not present
 * @author Patrick Ehlert
 * <p>
 * Created on 23-08-2018
 */
public class MissingDataException extends LoaderException {

    public MissingDataException(String msg) {
        super(msg);
    }

    public MissingDataException(String msg, Throwable t) {
        super(msg, t);
    }
}
