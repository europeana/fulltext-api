package eu.europeana.fulltext.batch.writer;

import eu.europeana.fulltext.api.service.FTService;
import java.util.List;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnoPageDeletionWriter implements ItemWriter<String> {

  private final FTService ftService;

  public AnnoPageDeletionWriter(FTService ftWriteService) {
    this.ftService = ftWriteService;
  }

  @Override
  public void write(@NonNull List<? extends String> annoPages) throws Exception {
    ftService.deleteAnnoPagesWithSources(annoPages);
  }
}
