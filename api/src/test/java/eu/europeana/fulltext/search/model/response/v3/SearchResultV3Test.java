package eu.europeana.fulltext.search.model.response.v3;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.fulltext.api.config.SerializationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SerializationConfig.class)
public class SearchResultV3Test {

    @Autowired
    private ObjectMapper objectMapper;

    private JacksonTester<SearchResultV3> json;

    @BeforeEach
    public void setup() {
        JacksonTester.initFields(this, objectMapper);
    }

    @Test
    public void testSerialization() throws IOException {
        final String SEARCH_ID = "/searchId/1";
        SearchResultV3 searchResult = new SearchResultV3(SEARCH_ID, false);

        JsonContent<SearchResultV3> serialized = json.write(searchResult);

        assertThat(serialized).extractingJsonPathStringValue("@.type")
                .isEqualTo("AnnotationPage");
        assertThat(serialized).extractingJsonPathStringValue("@.id")
                .isEqualTo(SEARCH_ID);
        assertThat(serialized).hasJsonPathArrayValue("@.items");
        assertThat(serialized).hasJsonPathArrayValue("@.hits");
    }
}