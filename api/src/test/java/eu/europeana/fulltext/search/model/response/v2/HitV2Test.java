package eu.europeana.fulltext.search.model.response.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.config.SerializationConfig;
import eu.europeana.fulltext.api.service.EDM2IIIFMapping;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {SerializationConfig.class, EDM2IIIFMapping.class, FTSettings.class})
public class HitV2Test {

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FTService ftService; // needed so we can initialize EDM2IIIFMapping

    private JacksonTester<HitV2> json;

    @BeforeEach
    public void setup() {
        JacksonTester.initFields(this, objectMapper);
    }

    @Test
    public void testSerialization() throws IOException {
        AnnoPage dummyPage = new AnnoPage();
        dummyPage.setRes(new Resource("rId", "en", "some text", null));
        Annotation dummyAnno = new Annotation("aId", AnnotationType.WORD.getAbbreviation(), 0, 4, null);

        HitV2 hit = new HitV2();
        hit.addAnnotation(0,4, dummyPage, dummyAnno);
        assertThat(json.write(hit)).hasJsonPathArrayValue("@.annotations");
        assertThat(json.write(hit)).hasJsonPathArrayValue("@.selectors");
        assertThat(json.write(hit)).extractingJsonPathStringValue("@.@type")
                .isEqualTo("search:Hit");
    }
}