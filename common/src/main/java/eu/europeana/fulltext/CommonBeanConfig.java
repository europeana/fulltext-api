package eu.europeana.fulltext;

import io.netty.handler.logging.LogLevel;
import java.util.Optional;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

public class CommonBeanConfig {


  public static WebClient createWebClient(Optional<String> annotationApiUrl, int maxBufferMb) {

    final ExchangeStrategies strategies =
        ExchangeStrategies.builder()
            .codecs(
                codecs ->
                    // see: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/codec/CodecConfigurer.DefaultCodecs.html#maxInMemorySize-int-
                    codecs.defaultCodecs().maxInMemorySize(maxBufferMb * 1024 * 1024))
            .build();

    return WebClient.builder()
        .exchangeStrategies(strategies)
        .baseUrl(annotationApiUrl.orElse(""))
        // used for logging Netty requests / responses.
        .clientConnector(
            new ReactorClientHttpConnector(
                HttpClient.create()
                    .followRedirect(true)
                    .wiretap(
                        HttpClient.class.getName(), LogLevel.TRACE, AdvancedByteBufFormat.TEXTUAL)))
        .build();
  }

}
