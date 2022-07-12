package eu.europeana.fulltext.migrations;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.migrations.repository.MigrationRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class MigrationAnnoPageWriter implements ItemWriter<AnnoPage> {

  private final MigrationRepository repository;

  public MigrationAnnoPageWriter(MigrationRepository repository) {
    this.repository = repository;
  }

  @Override
  public void write(@NotNull List<? extends AnnoPage> annoPages) throws Exception {

    List<Resource> resources =
        annoPages.stream().map(AnnoPage::getRes).collect(Collectors.toList());

    // Create new resource documents, since we can't update the _id of an existing record
    repository.save(resources);

    // update AnnoPage.res references
    repository.upsertAnnoPages(annoPages);

    // in a final step, we delete the old resources
    List<String> oldDbIds =
        annoPages.stream().map(a -> a.getRes().getOldDbId()).collect(Collectors.toList());
    repository.deleteResource(oldDbIds);

  }
}
