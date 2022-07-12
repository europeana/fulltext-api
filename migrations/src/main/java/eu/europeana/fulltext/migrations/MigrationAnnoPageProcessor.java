package eu.europeana.fulltext.migrations;

import static eu.europeana.fulltext.migrations.MigrationUtils.generateResourceId;
import static eu.europeana.fulltext.migrations.MigrationUtils.getRecordId;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class MigrationAnnoPageProcessor implements ItemProcessor<AnnoPage, AnnoPage> {

  @Override
  public AnnoPage process(@NotNull AnnoPage annoPage) throws Exception {
    String pageId = MigrationUtils.derivePageId(annoPage.getTgtId());
    // change the page ID
    annoPage.setPgId(pageId);

    // TODO: set Annotation IDs here
    List<Annotation> newAns =
        annoPage.getAns().stream().map(Function.identity()).collect(Collectors.toList());
    annoPage.setAns(newAns);

    Resource res = annoPage.getRes();
    String oldDbId = res.getId();
    res.setOldDbId(oldDbId);

    res.setId(generateResourceId(
        getRecordId(annoPage.getDsId(), annoPage.getLcId()),
        annoPage.getLang(),
        annoPage.getTgtId()));

    res.setPgId(pageId);

    return annoPage;
  }
}
