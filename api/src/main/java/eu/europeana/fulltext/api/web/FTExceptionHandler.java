package eu.europeana.fulltext.api.web;

import eu.europeana.api.commons.error.EuropeanaApiErrorResponse;
import eu.europeana.api.commons.error.EuropeanaApiErrorResponse.Builder;
import eu.europeana.api.commons.error.EuropeanaGlobalExceptionHandler;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Handles all uncaught exceptions thrown by the application.
 */
@ControllerAdvice
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
}
