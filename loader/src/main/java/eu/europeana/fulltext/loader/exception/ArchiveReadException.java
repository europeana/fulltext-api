package eu.europeana.fulltext.loader.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception that's thrown when there is a problem reading an archive (zip file)
 * @author Patrick Ehlert
 * <p>
 * Created on 23-08-2018
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ArchiveReadException extends LoaderException {

    private static final long serialVersionUID = -4445313907998967811L;

    public ArchiveReadException(String msg) {
        super(msg);
    }

    public ArchiveReadException(String msg, Throwable t) {
        super(msg, t);
    }

}
