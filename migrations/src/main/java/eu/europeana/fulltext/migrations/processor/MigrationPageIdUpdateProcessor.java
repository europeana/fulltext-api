package eu.europeana.fulltext.migrations.processor;

import static eu.europeana.fulltext.util.GeneralUtils.derivePageId;

import eu.europeana.fulltext.entity.AnnoPage;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class MigrationPageIdUpdateProcessor implements ItemProcessor<AnnoPage, AnnoPage> {

  @Override
  public AnnoPage process(AnnoPage annoPage) throws Exception {
    String pageId = derivePageId(annoPage.getTgtId());
    // change the page ID
    annoPage.setPgId(pageId);
    annoPage.getRes().setPgId(pageId);

    return annoPage;
  }
}
