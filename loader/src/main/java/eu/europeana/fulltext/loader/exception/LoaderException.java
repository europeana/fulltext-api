package eu.europeana.fulltext.loader.exception;



/**
 * Base error class for this application (needs work)
 * @author Lúthien
 * Created on 27-02-2018
 */
public class LoaderException extends Exception {

    public LoaderException(String msg, Throwable t) {
        super(msg, t);
    }

    public LoaderException(String msg) {
        super(msg);
    }

    /**
     * @return boolean indicating whether this type of exception should be logged or not
     */
    public boolean doLog() {
        return true; // default we log all exceptions
    }

}
