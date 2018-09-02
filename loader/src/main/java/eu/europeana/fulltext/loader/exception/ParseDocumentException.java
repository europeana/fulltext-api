package eu.europeana.fulltext.loader.exception;

/**
 * Exception that is thrown when the document cannot be parsed
 * @author Patrick Ehlert
 * <p>
 * Created on 24-08-2018
 */
public class ParseDocumentException extends LoaderException {

    public ParseDocumentException(String msg) {
        super(msg);
    }

    public ParseDocumentException(String msg, Throwable t) {
        super(msg, t);
    }
}
