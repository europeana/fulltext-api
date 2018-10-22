package eu.europeana.fulltext.loader.exception;

/**
 * Exception thrown we find multiple definitions of which there should be only 1
 * @author Patrick Ehlert
 * Created on 20-10-2018
 */
public class DuplicateDefinitionException extends LoaderException {

    public DuplicateDefinitionException(String msg) {
        super(msg);
    }

    public DuplicateDefinitionException(String msg, Throwable t) {
        super(msg, t);
    }
}
