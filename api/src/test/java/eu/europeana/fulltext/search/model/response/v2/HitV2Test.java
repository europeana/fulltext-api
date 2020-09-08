package eu.europeana.fulltext.search.model.response.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.fulltext.api.config.SerializationConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SerializationConfig.class)
public class HitV2Test {

    @Autowired
    private ObjectMapper objectMapper;

    private JacksonTester<HitV2> json;

    @Before
    public void setup() {
        JacksonTester.initFields(this, objectMapper);
    }

    @Test
    public void testSerialization() throws IOException {

        HitV2 hit = new HitV2(10, 100, "exact");
        assertThat(json.write(hit)).hasJsonPathArrayValue("@.annotations");
        assertThat(json.write(hit)).hasJsonPathArrayValue("@.selectors");
        assertThat(json.write(hit)).extractingJsonPathStringValue("@.@type")
                .isEqualTo("search:Hit");
    }
}