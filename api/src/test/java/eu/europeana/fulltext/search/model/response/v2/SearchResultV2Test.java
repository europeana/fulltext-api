package eu.europeana.fulltext.search.model.response.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.fulltext.api.config.SerializationConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SerializationConfig.class)
public class SearchResultV2Test {

    @Autowired
    private ObjectMapper objectMapper;

    private JacksonTester<SearchResultV2> json;

    @Before
    public void setup() {
        JacksonTester.initFields(this, objectMapper);
    }

    @Test
    public void testSerialization() throws IOException {
        final String SEARCH_ID = "/searchId/1";
        SearchResultV2 searchResult = new SearchResultV2(SEARCH_ID, false);

        JsonContent<SearchResultV2> serialized = json.write(searchResult);

        assertThat(serialized).extractingJsonPathStringValue("@.@type")
                .isEqualTo("sc:AnnotationList");
        assertThat(serialized).extractingJsonPathStringValue("@.@id")
                .isEqualTo(SEARCH_ID);
        assertThat(serialized).hasJsonPathArrayValue("@.resources");
        assertThat(serialized).hasJsonPathArrayValue("@.hits");
    }
}