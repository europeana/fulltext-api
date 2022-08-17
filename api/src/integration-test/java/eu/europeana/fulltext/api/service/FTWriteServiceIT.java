package eu.europeana.fulltext.api.service;

import static eu.europeana.fulltext.api.IntegrationTestUtils.ANNOPAGE_FILMPORTAL_1197365_EN_JSON;
import static eu.europeana.fulltext.api.IntegrationTestUtils.ANNOPAGE_FILMPORTAL_1197365_JSON;
import static eu.europeana.fulltext.api.IntegrationTestUtils.ANNOPAGE_VIMEO_208310501_JSON;
import static eu.europeana.fulltext.api.IntegrationTestUtils.loadFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.bulk.BulkWriteResult;
import eu.europeana.fulltext.api.BaseIntegrationTest;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Target;
import eu.europeana.fulltext.exception.DatabaseQueryException;
import eu.europeana.fulltext.subtitles.AnnotationPreview;
import eu.europeana.fulltext.util.GeneralUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FTWriteServiceIT extends BaseIntegrationTest {

  @Autowired FTService service;
  @Autowired ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    this.service.dropCollections();
  }

  @Test
  void saveAnnoPageShouldBeSuccessful() throws Exception {
    AnnoPage annoPage =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), AnnoPage.class);
    service.upsertAnnoPage(List.of(annoPage));
    assertEquals(1, service.countAnnoPage());
    assertEquals(1, service.countResource());
  }

  @Test
  void annoPageRetrievalShouldBeSuccessful() throws Exception {
    AnnoPage annoPage =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), AnnoPage.class);
    service.upsertAnnoPage(List.of(annoPage));

    AnnoPage retrievedAnnoPage =
        service.getAnnoPageByPgId("08604", "FDE2205EEE384218A8D986E5138F9691", "1", "nl", false);

    assertNotNull(retrievedAnnoPage.getRes());
  }

  @Test
  void saveAnnoPageBulkShouldBeSuccessful() throws Exception {
    assertEquals(0, service.countAnnoPage());

    AnnoPage annoPage1 =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), AnnoPage.class);
    AnnoPage annoPage2 =
        mapper.readValue(loadFile(ANNOPAGE_VIMEO_208310501_JSON), AnnoPage.class);

    service.upsertAnnoPage(List.of(annoPage1, annoPage2));

    assertEquals(2, service.countAnnoPage());
  }

  @Test
  void saveAnnoPageBulkShouldUpsert() throws Exception {

    AnnoPage annoPage1 =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), AnnoPage.class);
    service.upsertAnnoPage(List.of(annoPage1));

    assertEquals(1, service.countAnnoPage());

    // load AnnoPage again as Morphia sets _id on object during save
    AnnoPage annoPage1Copy =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), AnnoPage.class);

    // change random value in annoPage1
    Annotation newAnnotation =
        new Annotation(
            "65dacfd36f67b9b7faa983b514baf257", 'C', 851, 856, List.of(new Target(70947, 75000)));
    annoPage1Copy.getAns().add(newAnnotation);

    AnnoPage annoPage2 =
        mapper.readValue(loadFile(ANNOPAGE_VIMEO_208310501_JSON), AnnoPage.class);

    // try saving new annoPage (annoPage2) together with existing annoPage
    BulkWriteResult bulkWriteResult = service.upsertAnnoPage(List.of(annoPage2, annoPage1Copy));

    // should update the existing doc for annoPage1
    assertEquals(1, bulkWriteResult.getModifiedCount());

    // should also upsert the new doc
    assertEquals(1, bulkWriteResult.getUpserts().size());

    assertEquals(2, service.countAnnoPage());
  }

  @Test
  void shouldDropCollection() throws Exception {
    AnnoPage annoPage =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), AnnoPage.class);
    service.upsertAnnoPage(List.of(annoPage));
    assertEquals(1, service.countAnnoPage());

    service.dropCollections();

    assertEquals(0, service.countAnnoPage());
    assertEquals(0, service.countResource());
  }

  @Test
  void updateAnnoPageShouldBeSuccessful()
      throws Exception {
    assertEquals(0, service.countAnnoPage());
    assertEquals(0, service.countResource());

    // add the anno page and resource
    AnnoPage annoPage =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), AnnoPage.class);
    service.upsertAnnoPage(List.of(annoPage));
    assertEquals(1, service.countAnnoPage());
    assertEquals(1, service.countResource());

    String rights = annoPage.getRes().getRights() + "updated";
    // now update
    AnnotationPreview preview =
        new AnnotationPreview.Builder(
                GeneralUtils.generateRecordId(annoPage.getDsId(), annoPage.getLcId()),
                null,
                "")
            .setLanguage(annoPage.getLang())
            .setSource("https://annotation/source/value")
            .setRights(rights)
            .setMedia(annoPage.getTgtId())
            .setOriginalLang(false)
            .build();

    AnnoPage updatedAnnopage = service.updateAnnoPage(preview, annoPage);

    // check
    assertEquals("https://annotation/source/value", updatedAnnopage.getSource());
    assertEquals(rights, updatedAnnopage.getRes().getRights());
    assertEquals(annoPage.getAns().size(), updatedAnnopage.getAns().size());
    assertEquals(annoPage.getLang(), updatedAnnopage.getLang());
    assertEquals(1, service.countAnnoPage());
    assertEquals(1, service.countResource());
  }

  @Test
  void deprecateAnnoPageWithLangSuccessful() throws Exception {

    assertEquals(0, service.countAnnoPage());
    assertEquals(0, service.countResource());

    // add the anno page and resource
    AnnoPage annoPage =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), AnnoPage.class);
    service.upsertAnnoPage(List.of(annoPage));
    assertEquals(1, service.countAnnoPage());
    assertEquals(1, service.countResource());

    service.deprecateAnnoPages(
        annoPage.getDsId(), annoPage.getLcId(), annoPage.getPgId(), annoPage.getLang());

    // deprecation should set a "deleted" property on AnnoPage

    AnnoPage retrievedAnnoPage =
        service.getAnnoPageByPgId(annoPage.getDsId(), annoPage.getLcId(), annoPage.getPgId(), annoPage.getLang(), true);

    assertNotNull(retrievedAnnoPage);
    assertTrue(retrievedAnnoPage.isDeprecated());
  }

  @Test
  void deprecateAnnoPageWithoutLangSuccessful() throws Exception {

    assertEquals(0, service.countAnnoPage());
    assertEquals(0, service.countResource());

    AnnoPage annoPage =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), AnnoPage.class);
    service.upsertAnnoPage(List.of(annoPage));
    // add the anno page and resource with same dsId, lcId, pgId but different lang
    AnnoPage annoPage2 =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_EN_JSON), AnnoPage.class);
    service.upsertAnnoPage(List.of(annoPage2));

    assertEquals(2, service.countAnnoPage());
    assertEquals(2, service.countResource());

    // both annoppages have same dsId, lcId and pgId
    service.deprecateAnnoPages(annoPage.getDsId(), annoPage.getLcId(), annoPage.getPgId());

    AnnoPage retrievedAnnoPage1 =
        service.getAnnoPageByPgId(annoPage.getDsId(), annoPage.getLcId(), annoPage.getPgId(), annoPage.getLang(), true);
    assertNotNull(retrievedAnnoPage1);
    assertTrue(retrievedAnnoPage1.isDeprecated());

    AnnoPage retrievedAnnoPage2 =
        service.getAnnoPageByPgId(annoPage2.getDsId(), annoPage2.getLcId(), annoPage2.getPgId(), annoPage2.getLang(), true);
    assertNotNull(retrievedAnnoPage2);
    assertTrue(retrievedAnnoPage2.isDeprecated());
  }

  @Test
  void shouldDeprecateAnnoPagesViaSource() throws Exception{
    String source1 = "http://annotation/1";
    String source2 = "http://annotation/2";
    AnnoPage annoPage1 =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), AnnoPage.class);
    annoPage1.setSource(source1);
    service.upsertAnnoPage(List.of(annoPage1));

    AnnoPage annoPage2 =
        mapper.readValue(loadFile(ANNOPAGE_VIMEO_208310501_JSON), AnnoPage.class);
    annoPage2.setSource(source2);
    service.upsertAnnoPage(List.of(annoPage2));

    ftService.deprecateAnnoPagesWithSources(List.of(source1, source2));


    AnnoPage retrievedAnnoPage1 =
        service.getAnnoPageByPgId(annoPage1.getDsId(), annoPage1.getLcId(), annoPage1.getPgId(), annoPage1.getLang(), true);
    assertNotNull(retrievedAnnoPage1);
    assertTrue(retrievedAnnoPage1.isDeprecated());

    AnnoPage retrievedAnnoPage2 =
        service.getAnnoPageByPgId(annoPage2.getDsId(), annoPage2.getLcId(), annoPage2.getPgId(), annoPage2.getLang(), true);
    assertNotNull(retrievedAnnoPage2);
    assertTrue(retrievedAnnoPage2.isDeprecated());
  }
}
