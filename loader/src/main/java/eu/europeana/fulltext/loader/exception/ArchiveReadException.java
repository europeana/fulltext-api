package eu.europeana.fulltext.loader.exception;

/**
 * Exception that's thrown when there is a problem reading an archive (zip file)
 * @author Patrick Ehlert
 * <p>
 * Created on 23-08-2018
 */
public class ArchiveReadException extends LoaderException {

    public ArchiveReadException(String msg) {
        super(msg);
    }

    public ArchiveReadException(String msg, Throwable t) {
        super(msg, t);
    }

}
