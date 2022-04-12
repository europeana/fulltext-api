package eu.europeana.fulltext.search.web;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.api.FTApplication;
import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.search.exception.InvalidParameterException;
import eu.europeana.fulltext.search.model.query.EuropeanaId;
import eu.europeana.fulltext.search.model.response.v2.SearchResultV2;
import eu.europeana.fulltext.search.model.response.v3.SearchResultV3;
import eu.europeana.fulltext.search.service.FTSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_IIIF_V2;
import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_IIIF_V3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = FTApplication.class)
@WebMvcTest(value = FTSearchController.class,
// disable security for this test
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class
    })
public class FTSearchControllerTest {

    @MockBean
    private FTSearchService searchService;

    @MockBean
    private FTSettings ftSettings;

    @Autowired
    private MockMvc mockMvc;

    private final String TEST_SEARCH_ID = "test_search_id";


    @BeforeEach
    public void setUp() throws Exception {
        SearchResultV2 testResultV2 = new SearchResultV2(TEST_SEARCH_ID, false);
        SearchResultV3 testResultV3 = new SearchResultV3(TEST_SEARCH_ID, false);

        // return SearchResultV2 when requestVersion is 2
        when(searchService.searchIssue(anyString(), any(EuropeanaId.class), anyString(), anyInt(), any(List.class),
                eq("2"), anyBoolean())).thenReturn(
                        testResultV2
        );

        // return SearchResultV3 when requestVersion is 3
        when(searchService.searchIssue(anyString(), any(EuropeanaId.class), anyString(), anyInt(), any(List.class),
                eq("3"), anyBoolean())).thenReturn(
                        testResultV3
        );
    }

    @Test
    public void shouldReturnErrorOnInvalidQueryPageSize() throws Exception {
        mockMvc.perform(get("/presentation/9200355/BibliographicResource_3000096341989/search")
                .param("q", "ster")
                .param("pageSize", "0"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void shouldReturnErrorOnInvalidFormatParam() throws Exception {
        mockMvc.perform(get("/presentation/9200355/BibliographicResource_3000096341989/search")
                .param("q", "ster")
                // EA-2181: only 2 and 3 currently supported
                .param("format", "5"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void shouldReturnErrorOnInvalidAcceptHeader() throws Exception {
        mockMvc.perform(get("/presentation/9200355/BibliographicResource_3000096341989/search")
                .accept("application/json;profile=\"invalidProfile\"")
                .param("q", "ster"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void shouldReturnV2ResponseWhenAcceptHeaderContainsProfile() throws Exception {
        mockMvc.perform(get("/presentation/9200355/BibliographicResource_3000096341989/search")
                .accept("application/json;profile=\"" + MEDIA_TYPE_IIIF_V2 + "\"")
                .param("q", "testQuery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@id").value(TEST_SEARCH_ID))
                .andExpect(jsonPath("$.@type").value("sc:AnnotationList"));
    }

    @Test
    public void shouldReturnV3ResponseWhenAcceptHeaderContainsProfile() throws Exception {
        mockMvc.perform(get("/presentation/9200355/BibliographicResource_3000096341989/search")
                .accept("application/json;profile=\"" + MEDIA_TYPE_IIIF_V3 + "\"")
                .param("q", "testQuery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_SEARCH_ID))
                .andExpect(jsonPath("$.type").value("AnnotationPage"));
    }

    @Test
    public void shouldReturnV2ResponseWhenNoFormatSpecified() throws Exception {
        mockMvc.perform(get("/presentation/9200355/BibliographicResource_3000096341989/search")
                .param("q", "testQuery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@id").value(TEST_SEARCH_ID))
                .andExpect(jsonPath("$.@type").value("sc:AnnotationList"));
    }

    @Test
    public void shouldReturnV3ResponseIfFormatIs3() throws Exception {
        mockMvc.perform(get("/presentation/9200355/BibliographicResource_3000096341989/search")
                .param("q", "testQuery")
                .param("format", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_SEARCH_ID))
                .andExpect(jsonPath("$.type").value("AnnotationPage"));
    }

    @Test
    public void testValidateTextGranularityNoValue() throws InvalidParameterException {
        List<AnnotationType> defaultAnnoTypes = new ArrayList<>(){{
            add(AnnotationType.WORD);
        }};
        when(ftSettings.getDefaultSearchTextGranularity()).thenReturn(defaultAnnoTypes);

        FTSearchController searchController = new FTSearchController(searchService, ftSettings);
        List<AnnotationType> annoTypes = searchController.validateTextGranularity(null);
        assertEquals(defaultAnnoTypes, annoTypes);
    }


}