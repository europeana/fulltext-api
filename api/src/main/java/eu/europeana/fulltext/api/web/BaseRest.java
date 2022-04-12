package eu.europeana.fulltext.api.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.commons.service.authorization.AuthorizationService;
import eu.europeana.api.commons.web.controller.BaseRestController;
import eu.europeana.api.commons.web.service.AbstractRequestPathMethodService;
import eu.europeana.fulltext.entity.AnnoPage;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class BaseRest extends BaseRestController {
  @Autowired private AuthorizationService emAuthorizationService;
  @Autowired private AbstractRequestPathMethodService requestPathMethodService;
  @Autowired private ObjectMapper mapper;

  protected AuthorizationService getAuthorizationService() {
    return emAuthorizationService;
  }

  protected ResponseEntity<String> generateResponse(
      HttpServletRequest request, String responseBody, HttpStatus status) {

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.ALLOW, getMethodsForRequestPattern(request, requestPathMethodService));
    headers.add(
        HttpHeaders.CONTENT_TYPE,
        eu.europeana.api.commons.web.http.HttpHeaders.CONTENT_TYPE_JSONLD);

    return ResponseEntity.status(status).headers(headers).body(responseBody);
  }

  protected ResponseEntity<String> noContentResponse(HttpServletRequest request) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.ALLOW, getMethodsForRequestPattern(request, requestPathMethodService));
    return ResponseEntity.noContent().headers(headers).build();
  }

  protected String serializeJsonLd(AnnoPage annoPage) throws JsonProcessingException {
    return mapper.writeValueAsString(annoPage);
  }

  protected <T> String serializeResponse(T responseObject) throws JsonProcessingException {
    return mapper.writeValueAsString(responseObject);
  }
}
