package eu.europeana.fulltext.batch.writer;

import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.entity.AnnoPage;
import java.util.List;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnoPageUpsertWriter implements ItemWriter<AnnoPage> {

  private final FTService ftService;

  public AnnoPageUpsertWriter(FTService annotationService) {
    this.ftService = annotationService;
  }

  @Override
  public void write(@NonNull List<? extends AnnoPage> annoPages) throws Exception {
    ftService.upsertAnnoPage(annoPages);
  }
}
