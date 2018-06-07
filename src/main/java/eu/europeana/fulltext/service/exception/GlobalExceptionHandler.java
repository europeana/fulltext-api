package eu.europeana.fulltext.service.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler that catches all errors and logs the interesting ones
 * @author Lúthien
 * Created on 27-02-2018
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LogManager.getLogger(GlobalExceptionHandler.class);

    // TODO make sure we return xml errors messages instead of json

    /**
     * Checks if we should log an error and rethrows it
     * @param e
     * @throws FTException
     */
    @ExceptionHandler(FTException.class)
    public void handleFulltextException(FTException e) throws FTException {
        if (e.doLog()) {
            LOG.error(e.getMessage(), e);
        }
        throw e;
    }
}
