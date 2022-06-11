package eu.europeana.fulltext.api.web;

import static eu.europeana.fulltext.api.IntegrationTestUtils.ANNOPAGE_FILMPORTAL_1197365_JSON;
import static eu.europeana.fulltext.api.IntegrationTestUtils.loadFile;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.europeana.fulltext.WebConstants;
import eu.europeana.fulltext.api.BaseIntegrationTest;
import eu.europeana.fulltext.entity.AnnoPage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

@SpringBootTest
@AutoConfigureMockMvc
class FulltextRetrievalIT extends BaseIntegrationTest {

  @Test
  void retrievingDeprecatedAnnoPageShouldReturn410() throws Exception {
    AnnoPage annoPage =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), AnnoPage.class);

    ftService.saveAnnoPage(annoPage);
    // manually deprecate AnnoPage
    ftService.deprecateAnnoPages(
        annoPage.getDsId(), annoPage.getLcId(), annoPage.getPgId(), annoPage.getLang());

    mockMvc
        .perform(
            get(
                    "/presentation/{dsId}/{lcId}/annopage/{pageId}",
                    annoPage.getDsId(),
                    annoPage.getLcId(),
                    annoPage.getPgId())
                .param(WebConstants.REQUEST_VALUE_LANG, annoPage.getLang())
                .accept(MediaType.APPLICATION_JSON))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isGone());
  }
}
