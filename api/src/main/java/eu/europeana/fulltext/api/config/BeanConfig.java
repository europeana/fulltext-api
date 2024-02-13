package eu.europeana.fulltext.api.config;

import eu.europeana.api.commons.oauth2.service.impl.EuropeanaClientDetailsService;
import eu.europeana.fulltext.CommonBeanConfig;
import eu.europeana.fulltext.service.AnnotationApiRestService;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

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

  //A component required a bean named 'mvcConversionService' that could not be found.
  //Caused by: org.springframework.beans.factory.BeanNotOfRequiredTypeException: Bean named 'mvcConversionService' is expected to be of type 'org.springframework.format.support.FormattingConversionService'
  // but was actually of type 'org.springframework.core.convert.support.DefaultConversionService'
  @Bean
  public ConversionService mvcConversionService() {
    return new FormattingConversionService();
  }

  @Bean
  public RequestMappingHandlerMapping requestMappingHandlerMapping() {
    return  new RequestMappingHandlerMapping();
  }
}
