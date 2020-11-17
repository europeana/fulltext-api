package eu.europeana.fulltext.api.web;

import eu.europeana.api.commons.error.EuropeanaGlobalExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Handles all uncaught exceptions thrown by the application.
 */
@ControllerAdvice
public class FTExceptionHandler extends EuropeanaGlobalExceptionHandler {
    // exception handling inherited from parent
}
