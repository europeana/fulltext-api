package eu.europeana.fulltext.api.service.exception;



/**
 * Base error class for this application (needs work)
 * @author Lúthien
 * Created on 27-02-2018
 */
class FTException extends Exception {

    private static final long serialVersionUID = 6584353234989077456L;
    private ErrorCode errorCode;

    public FTException(String msg, Throwable t) {
        super(msg, t);
    }

    public FTException(String msg, ErrorCode errorCode) {
        super(msg);
        this.errorCode = errorCode;
    }

    public FTException(String msg) {
        super(msg);
    }

    /**
     * @return boolean indicating whether this type of exception should be logged or not
     */
    public boolean doLog() {
        return true; // default we log all exceptions
    }

}
