package eu.europeana.fulltext.loader.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler that catches all errors and logs the interesting ones
 * @author Patrick Ehlert
 * @deprecated 2023
 * Created on 20-02-2018
 */
@Deprecated
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LogManager.getLogger(GlobalExceptionHandler.class);

    /**
     * Checks if we should log an error and rethrows it
     * @param e
     * @throws LoaderException
     */
    @ExceptionHandler(LoaderException.class)
    public void handleIiifException(LoaderException e) throws LoaderException {
        if (e.doLog()) {
            LOG.error(e.getMessage(), e);
        }
        throw e;
    }
}
