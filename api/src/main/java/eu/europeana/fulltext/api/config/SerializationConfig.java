package eu.europeana.fulltext.api.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import ioinformarics.oss.jackson.module.jsonld.JsonldModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Configure Jackson serialization output.
 */
@Configuration
public class SerializationConfig {

    @Bean
    public ObjectMapper mapper() {
        return new Jackson2ObjectMapperBuilder()
                // ignore null properties when serializing objects
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY).build();
    }

    @Bean
    public com.fasterxml.jackson.databind.Module jsonldModule() {
        return new JsonldModule();
    }
}
