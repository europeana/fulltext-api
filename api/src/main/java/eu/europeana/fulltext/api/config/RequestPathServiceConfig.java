package eu.europeana.fulltext.api.config;

import eu.europeana.api.commons.web.service.AbstractRequestPathMethodService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
public class RequestPathServiceConfig extends AbstractRequestPathMethodService {

  protected RequestPathServiceConfig(
      WebApplicationContext applicationContext) {
    super(applicationContext);
  }
}
