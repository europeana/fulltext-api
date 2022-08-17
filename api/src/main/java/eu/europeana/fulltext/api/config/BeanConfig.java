package eu.europeana.fulltext.api.config;

import eu.europeana.api.commons.oauth2.service.impl.EuropeanaClientDetailsService;
import io.netty.handler.logging.LogLevel;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

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
  public SpringTemplateEngine springTemplateEngine() {
    SpringTemplateEngine springTemplateEngine = new SpringTemplateEngine();
    springTemplateEngine.addTemplateResolver(emailTemplateResolver());
    return springTemplateEngine;
  }

  @Bean
  public WebClient webClient() {

    final ExchangeStrategies strategies =
        ExchangeStrategies.builder()
            .codecs(
                codecs ->
                    // see: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/codec/CodecConfigurer.DefaultCodecs.html#maxInMemorySize-int-
                    codecs.defaultCodecs().maxInMemorySize(settings.getMaxBufferMb() * 1024 * 1024))
            .build();

    return WebClient.builder()
        .exchangeStrategies(strategies)
        .baseUrl(settings.getAnnotationsApiUrl())
        // used for logging Netty requests / responses.
        .clientConnector(
            new ReactorClientHttpConnector(
                HttpClient.create()
                    .followRedirect(true)
                    .wiretap(
                        HttpClient.class.getName(), LogLevel.TRACE, AdvancedByteBufFormat.TEXTUAL)))
        .build();
  }

  private ClassLoaderTemplateResolver emailTemplateResolver() {
    ClassLoaderTemplateResolver emailTemplateResolver = new ClassLoaderTemplateResolver();
    emailTemplateResolver.setPrefix("/templates/");
    emailTemplateResolver.setSuffix(".html");
    emailTemplateResolver.setTemplateMode(TemplateMode.HTML);
    emailTemplateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
    emailTemplateResolver.setCacheable(false);
    return emailTemplateResolver;
  }
}
