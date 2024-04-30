package eu.europeana.fulltext.api.web;

import eu.europeana.api.commons.error.EuropeanaApiErrorResponse;
import eu.europeana.api.commons.error.EuropeanaApiErrorResponse.Builder;
import eu.europeana.api.commons.web.exception.ApplicationAuthenticationException;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;

import eu.europeana.api.commons.web.exception.EuropeanaGlobalExceptionHandler;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Handles all uncaught exceptions thrown by the application.
 */
@ControllerAdvice
@EnableWebMvc
public class FTExceptionHandler extends EuropeanaGlobalExceptionHandler {

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<EuropeanaApiErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException e, HttpServletRequest httpRequest) {
    HttpStatus responseStatus = HttpStatus.BAD_REQUEST;

    EuropeanaApiErrorResponse response =
        new Builder(httpRequest, e, this.stackTraceEnabled())
            .setStatus(responseStatus.value())
            .setError(responseStatus.getReasonPhrase())
            // e.getMessage() includes stacktrace
            .setMessage("Required request body is missing")
            .build();

    return ResponseEntity.status(responseStatus)
        .contentType(MediaType.APPLICATION_JSON)
        .body(response);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<EuropeanaApiErrorResponse> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException e, HttpServletRequest httpRequest) {
    HttpStatus responseStatus = HttpStatus.BAD_REQUEST;

    String errorMessage =
        String.format(
            "'%s' should be of type %s",
            e.getName(), e.getParameter().getGenericParameterType().getTypeName());
    EuropeanaApiErrorResponse response =
        new Builder(httpRequest, e, this.stackTraceEnabled())
            .setStatus(responseStatus.value())
            .setError(responseStatus.getReasonPhrase())
            .setMessage(errorMessage)
            .build();

    return ResponseEntity.status(responseStatus)
        .contentType(MediaType.APPLICATION_JSON)
        .body(response);
  }


  /**
   * Handle AuthenticationException thrown when API Key or JWT validation fails.
   * This handles a different type from {@link EuropeanaGlobalExceptionHandler#handleAuthenticationError(AuthenticationException, HttpServletRequest)}
   *
   * TODO: move to api-commons
   */
  @ExceptionHandler
  public ResponseEntity<EuropeanaApiErrorResponse> handleApplicationAuthenticationException(
      ApplicationAuthenticationException e, HttpServletRequest httpRequest) {
    HttpStatus responseStatus = HttpStatus.UNAUTHORIZED;
    EuropeanaApiErrorResponse errorResponse =
        new Builder(httpRequest, e, this.stackTraceEnabled())
            .setStatus(responseStatus.value())
            .setError(responseStatus.getReasonPhrase())
            // e.i18nParams contains cause(s) of failure
            .setMessage(Strings.join(Arrays.asList(e.getI18nParams()), ','))
            .build();
    return ResponseEntity.status(responseStatus)
        .contentType(MediaType.APPLICATION_JSON)
        .body(errorResponse);
  }
}
