package eu.europeana.fulltext.loader.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception that's thrown when an archive file was not found
 * @author Patrick Ehlert
 * <p>
 * Created on 12-01-2019
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ArchiveNotFoundException extends LoaderException {

    public ArchiveNotFoundException(String msg) {
        super(msg);
    }

    public ArchiveNotFoundException(String msg, Throwable t) {
        super(msg, t);
    }

    /**
     * @return boolean indicating whether this type of exception should be logged or not
     */
    public boolean doLog() {
        return false;
    }

}
