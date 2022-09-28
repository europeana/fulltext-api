package eu.europeana.fulltext.api.config;

import eu.europeana.api.commons.error.EuropeanaApiErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static eu.europeana.iiif.Definitions.MEDIA_TYPE_JSONLD;

/**
 * This will set json-ld as the default type if there is no accept header specified or if it's *
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
  
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // application/ld+json should take precedence over application/json
        configurer.defaultContentType(MediaType.valueOf(MEDIA_TYPE_JSONLD), MediaType.APPLICATION_JSON);
    }

    /**
     * Use custom fields in Error responses
     */
    @Bean
    public ErrorAttributes errorAttributes() {
        return new EuropeanaApiErrorAttributes();
    }
}
