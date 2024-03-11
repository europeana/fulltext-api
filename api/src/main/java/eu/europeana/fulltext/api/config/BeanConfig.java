package eu.europeana.fulltext.api.config;

import eu.europeana.api.commons.oauth2.service.impl.EuropeanaClientDetailsService;
import eu.europeana.fulltext.CommonBeanConfig;
import eu.europeana.fulltext.service.AnnotationApiRestService;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

  private final FTSettings settings;

  public BeanConfig(FTSettings settings) {
    this.settings = settings;
  }

  @Bean
  public EuropeanaClientDetailsService clientDetailsService() {
    EuropeanaClientDetailsService clientDetailsService = new EuropeanaClientDetailsService();
    clientDetailsService.setApiKeyServiceUrl(settings.getApiKeyUrl());
    return clientDetailsService;
  }


  @Bean
  public AnnotationApiRestService annotationApiRestService() {
    return new AnnotationApiRestService(CommonBeanConfig.createWebClient(
        Optional.empty(), settings.getMaxBufferMb()),
        settings.getAnnotationsApiKey());
  }
}
