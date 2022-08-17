package eu.europeana.fulltext.migrations.processor;

import static eu.europeana.fulltext.util.GeneralUtils.createAnnotationHash;
import static eu.europeana.fulltext.util.GeneralUtils.derivePageId;
import static eu.europeana.fulltext.util.GeneralUtils.generateRecordId;
import static eu.europeana.fulltext.util.GeneralUtils.generateResourceId;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class MigrationAnnoPageProcessor implements ItemProcessor<AnnoPage, AnnoPage> {

  @Override
  public AnnoPage process(@NotNull AnnoPage annoPage) throws Exception {
    String pageId = derivePageId(annoPage.getTgtId());
    // change the page ID
    annoPage.setPgId(pageId);

    for (Annotation a : annoPage.getAns()) {
      // keep track of old annotationId in case we need to update it directly
      a.setOldAnId(a.getAnId());
      String annoId = createAnnotationHash(a, annoPage.getTgtId(), annoPage.getLang());
      a.setAnId(annoId);
    }

    Resource res = annoPage.getRes();
    String oldDbId = res.getId();
    res.setOldDbId(oldDbId);

    res.setId(
        generateResourceId(
            generateRecordId(annoPage.getDsId(), annoPage.getLcId()),
            annoPage.getLang(),
            annoPage.getTgtId()));

    res.setPgId(pageId);

    return annoPage;
  }
}
