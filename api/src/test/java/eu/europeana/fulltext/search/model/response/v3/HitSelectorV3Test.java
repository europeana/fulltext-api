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
public class HitSelectorV3Test {

    @Autowired
    private ObjectMapper objectMapper;

    private JacksonTester<HitSelectorV3> json;

    @BeforeEach
    public void setup() {
        JacksonTester.initFields(this, objectMapper);
    }

    @Test
    public void testSerialization() throws IOException {
        JsonContent<HitSelectorV3> serialized = json.write(new HitSelectorV3("", "word", ""));
        assertThat(serialized).extractingJsonPathStringValue("@.type")
                .isEqualTo("TextQuoteSelector");
    }
}