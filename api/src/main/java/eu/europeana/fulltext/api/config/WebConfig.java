package eu.europeana.fulltext.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_JSONLD;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * This will set json-ld as the default type if there is no accept header specified or if it's *
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // application/ld+json should take precedence over application/json
        configurer.defaultContentType(MediaType.valueOf(MEDIA_TYPE_JSONLD), MediaType.APPLICATION_JSON);
    }

}