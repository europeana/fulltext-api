package eu.europeana.fulltext.test;

import eu.europeana.fulltext.FTApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test the application's controller
 * @author Lúthien
 * Created on 28-02-2018
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes={FTApplication.class})
public class FTControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    /**
     * Loads the entire webapplication as mock server
     */
    @Before
    public void setup() {
        // TODO instead of loading the entire application as a mock server, perhaps we can limit ourselves to the fulltextController part
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();
        assertThat(this.mockMvc).isNotNull();
    }

    @Test
    public void testIdentify() throws Exception {
        this.mockMvc.perform(get("/ft?verb=Identify").accept(MediaType.parseMediaType("text/xml")))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetRecord() throws Exception {
        this.mockMvc.perform(get("/ft?verb=GetRecord&metadataPrefix=edm&identifier=90402/BK_1978_399").accept(MediaType.parseMediaType("text/xml")))
                .andExpect(status().isOk());
    }

    @Test
    public void testInvalidVerb() throws Exception {
        this.mockMvc.perform(get("/ft?verb=XXX").accept(MediaType.parseMediaType("text/xml")))
                .andExpect(status().isNotFound());
    }
}
