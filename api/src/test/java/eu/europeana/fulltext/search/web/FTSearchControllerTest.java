package eu.europeana.fulltext.search.web;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.api.FTApplication;
import eu.europeana.fulltext.search.model.query.EuropeanaId;
import eu.europeana.fulltext.search.model.response.v2.SearchResultV2;
import eu.europeana.fulltext.search.model.response.v3.SearchResultV3;
import eu.europeana.fulltext.search.service.FTSearchService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static eu.europeana.fulltext.TestUtils.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FTApplication.class)
@WebMvcTest(FTSearchController.class)
public class FTSearchControllerTest {

    @MockBean
    private FTSearchService searchService;

    @Autowired
    private MockMvc mockMvc;

    private final String TEST_SEARCH_ID = "test_search_id";


    @Before
    public void setUp() throws Exception {
        SearchResultV2 testResultV2 = new SearchResultV2(TEST_SEARCH_ID, false);
        Collections.addAll(testResultV2.getItems(), annv2_1, annv2_2, annv2_3);

        SearchResultV3 testResultV3 = new SearchResultV3(TEST_SEARCH_ID, false);
        Collections.addAll(testResultV3.getItems(), annv3_1, annv3_2);

        // return SearchResultV2 when requestVersion is 2
        when(searchService.searchIssue(anyString(), any(EuropeanaId.class), anyString(), anyInt(), any(AnnotationType.class), anyBoolean(), eq("2"))).thenReturn(
                testResultV2
        );

        // return SearchResultV2 when requestVersion is 3
        when(searchService.searchIssue(anyString(), any(EuropeanaId.class), anyString(), anyInt(), any(AnnotationType.class), anyBoolean(), eq("3"))).thenReturn(
                testResultV3
        );
    }

    @Test
    public void shouldThrowOnInvalidQueryPageSize() throws Exception {
        mockMvc.perform(get("/presentation/9200355/BibliographicResource_3000096341989/search")
                .param("q", "ster")
                .param("pageSize", "0"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void shouldReturnV2ResponseWhenNoFormatSpecified() throws Exception {
        mockMvc.perform(get("/presentation/9200355/BibliographicResource_3000096341989/search")
                .param("q", "testQuery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@id").value(TEST_SEARCH_ID))
                .andExpect(jsonPath("$.@type").value("sc:AnnotationList"))
                // 3 annotations in testResultV2
                .andExpect(jsonPath("$.@resources.length()").value(3));
    }

    @Test
    public void shouldReturnV3ResponseIfFormatIs3() throws Exception {
        mockMvc.perform(get("/presentation/9200355/BibliographicResource_3000096341989/search")
                .param("q", "testQuery")
                .param("format", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_SEARCH_ID))
                .andExpect(jsonPath("$.type").value("AnnotationPage"))
                // 2 annotations in testResultV3
                .andExpect(jsonPath("$.items.length()").value(2));
    }
}