package eu.europeana.fulltext.migrations;

import static eu.europeana.fulltext.util.GeneralUtils.getAnnoPageObjectIds;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.migrations.repository.MigrationRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class MigrationAnnoPageWriter implements ItemWriter<AnnoPage> {

  private final MigrationRepository repository;
  private static final Logger logger = LogManager.getLogger(MigrationAnnoPageWriter.class);

  public MigrationAnnoPageWriter(MigrationRepository repository) {
    this.repository = repository;
  }

  @Override
  public void write(@NotNull List<? extends AnnoPage> annoPages) throws Exception {

    if (logger.isDebugEnabled()) {
      logger.debug(
          "AnnoPage write starting..., objectIds={}",
          Arrays.toString(getAnnoPageObjectIds(annoPages)));
    }
    List<Resource> resources =
        annoPages.stream().map(AnnoPage::getRes).collect(Collectors.toList());

    // Create new resource documents, since we can't update the _id of an existing record
    repository.save(resources);

    // update AnnoPage.res references
    repository.upsertAnnoPages(annoPages);

    // in a final step, we delete the old resources
    List<String> oldDbIds =
        annoPages.stream()
            // prevent deletion of new Resources if this job is run multiple times
            .filter(a -> !Objects.equals(a.getRes().getOldDbId(), a.getRes().getId()))
            .map(a -> a.getRes().getOldDbId())
            .collect(Collectors.toList());
    repository.deleteResource(oldDbIds);
  }
}
