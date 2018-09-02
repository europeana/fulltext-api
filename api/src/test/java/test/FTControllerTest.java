package test;

import eu.europeana.fulltext.api.FTApplication;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the application's controller
 * @author Lúthien
 * Created on 28-02-2018
 */
@Ignore
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
//        this.mockMvc.perform(get("/ft?verb=Identify").accept(MediaType.parseMediaType("text/xml")))
//                .andExpect(status().isOk());
    }


}
