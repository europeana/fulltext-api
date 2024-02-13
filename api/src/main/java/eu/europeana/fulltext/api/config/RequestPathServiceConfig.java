package eu.europeana.fulltext.api.config;

import eu.europeana.api.commons.web.service.AbstractRequestPathMethodService;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
public class RequestPathServiceConfig extends AbstractRequestPathMethodService {

//https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.7-Release-Notes#spring-mvcs-requestmappinghandlermapping-is-no-longer-primary
    protected RequestPathServiceConfig(WebApplicationContext applicationContext) {
        super(applicationContext);
    }


}