package eu.europeana.fulltext.loader.exception;

/**
 * Exception thrown when a field or attribute value is incorrect
 * @author Patrick Ehlert
 * Created on 20-10-2018
 */
public class IllegalValueException extends LoaderException {

    public IllegalValueException(String msg) {
        super(msg);
    }

    public IllegalValueException(String msg, Throwable t) {
        super(msg, t);
    }
}
