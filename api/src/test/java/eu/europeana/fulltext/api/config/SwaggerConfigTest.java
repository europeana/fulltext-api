package eu.europeana.fulltext.api.config;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JUnit test to check if Swagger is setup fine
 */
@SpringBootTest
@AutoConfigureMockMvc
@Disabled("Disabled as this loads the entire application. Should be made an integration test")
class SwaggerConfigTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test if the /v3/api-docs endpoint is available and if CORS is enabled for it
     */
    @Test
    void testApiDocEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                .header(HttpHeaders.ORIGIN, "https://test.com"))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"))
                // check if api info is loaded properly and contains minimally contact email and license information
                .andExpect(jsonPath("$.info.contact.email").exists())
                .andExpect(jsonPath("$.info.contact.email").value("api@europeana.eu"))
                .andExpect(jsonPath("$.info.license.name").exists())
                .andExpect(jsonPath("$.info.license.name").value("EUPL 1.2"));
    }

    /**
     * Test if Swagger UI is available
     */
    @Test
    void testSwaggerUI() throws Exception {
        mockMvc.perform(get("/swagger-ui/"))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

}
