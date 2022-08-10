package eu.europeana.fulltext.api.service;

import static eu.europeana.fulltext.AppConstants.ANNOTATION_DELETED_PATH;
import static eu.europeana.fulltext.AppConstants.ANNOTATION_SEARCH_PATH;

import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.exception.AnnotationApiGoneException;
import eu.europeana.fulltext.exception.AnnotationApiNotFoundException;
import eu.europeana.fulltext.subtitles.external.AnnotationItem;
import eu.europeana.fulltext.subtitles.external.AnnotationSearchResponse;
import eu.europeana.fulltext.util.GeneralUtils;
import io.netty.handler.logging.LogLevel;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.Exceptions;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

@Service
public class AnnotationApiRestService {
  private final WebClient webClient;
  private static final Logger logger = LogManager.getLogger(AnnotationApiRestService.class);

  /** Date format used by Annotation API for to and from param in deleted endpoint */
  private final DateTimeFormatter deletedDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

  private final String wskey;

  public AnnotationApiRestService(FTSettings settings, WebClient webClient) {
    this.wskey = settings.getAnnotationsApiKey();
    this.webClient = webClient;
  }

  public List<AnnotationItem> getAnnotations(int page, int pageSize, Instant from, Instant to) {
    String searchQuery = GeneralUtils.generateAnnotationSearchQuery(from, to);
    AnnotationSearchResponse response =
        webClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path(ANNOTATION_SEARCH_PATH)
                        .queryParam("query", searchQuery)
                        .queryParam("wskey", wskey)
                        .queryParam("sort", "created")
                        .queryParam("sortOrder", "asc")
                        .queryParam("page", page)
                        .queryParam("pageSize", pageSize)
                        .build())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(AnnotationSearchResponse.class)
            .block();

    if (response == null) {
      logger.warn("AnnotationSearchResponse not deserialized");
      return Collections.emptyList();
    }

    List<AnnotationItem> items = response.getItems();

    if (logger.isDebugEnabled()) {
      int fetchedItems = items == null ? 0 : items.size();
      logger.debug("Retrieved {} annotations; totalItems={} ", fetchedItems, response.getTotal());
    }

    return items;
  }

  public Optional<AnnotationItem> retrieveAnnotation(String annotationId)
      throws AnnotationApiNotFoundException {
    // add wskey to request
    String uri = annotationId + "?wskey=" + wskey;

    try {
      return Optional.ofNullable(
          webClient
              .get()
              .uri(URI.create(uri))
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              // throw custom exception so we can handle 410 and 404 responses separately

              .onStatus(
                  HttpStatus.GONE::equals,
                  response ->
                      response.bodyToMono(String.class).map(AnnotationApiGoneException::new))
              .onStatus(
                  HttpStatus.NOT_FOUND::equals,
                  response ->
                      response.bodyToMono(String.class).map(AnnotationApiNotFoundException::new))
              .bodyToMono(AnnotationItem.class)
              .block());
    } catch (Exception e) {
      /*
       * Spring WebFlux wraps exceptions in ReactiveError (see Exceptions.propagate())
       * So we need to unwrap the underlying exception, for it to be handled by callers of this method
       **/
      Throwable t = Exceptions.unwrap(e);

      // return empty optional if annotation has been deleted on Annotation API
      if (t instanceof AnnotationApiGoneException) {
        return Optional.empty();
      }

      if (t instanceof AnnotationApiNotFoundException) {
        // rethrow Not Found error so @ControllerAdvice can handle it correctly
        throw new AnnotationApiNotFoundException("Annotation does not exist");
      }

      // all other exceptions should be propagated
      throw e;
    }
  }

  public List<String> getDeletedAnnotations(int page, int pageSize, Instant from, Instant to) {
    List<String> deletedAnnotations =
        webClient
            .get()
            .uri(buildUriForDeletedAnnotations(page, pageSize, from, to))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
            .block();

    if (logger.isDebugEnabled()) {
      logger.debug("Retrieved deleted annotations ids={}", deletedAnnotations);
    }
    return deletedAnnotations;
  }


  /**
   * Helper method for constructing request URI. Only includes "from" and "to" parameters if not
   * null.
   */
  private Function<UriBuilder, URI> buildUriForDeletedAnnotations(
      int page, int pageSize, Instant from, Instant to) {
    return uriBuilder -> {
      UriBuilder builder =
          uriBuilder
              .path(ANNOTATION_DELETED_PATH)
              .queryParam("wskey", wskey)
              .queryParam("page", page)
              .queryParam("limit", pageSize);

      if (from != null) {
        builder.queryParam("from", from.atZone(ZoneOffset.UTC).format(deletedDateFormat));
      }

      if (to != null) {
        builder.queryParam("to", to.atZone(ZoneOffset.UTC).format(deletedDateFormat));
      }

      return builder.build();
    };
  }
}
