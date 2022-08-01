package eu.europeana.fulltext.migrations;

import static eu.europeana.fulltext.util.GeneralUtils.getAnnoPageObjectIds;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.migrations.repository.MigrationRepository;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class MigrationPageIdUpdateWriter implements ItemWriter<AnnoPage> {

  private final MigrationRepository repository;
  private static final Logger logger = LogManager.getLogger(MigrationPageIdUpdateWriter.class);

  public MigrationPageIdUpdateWriter(MigrationRepository repository) {
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

    repository.updateResourcePgId(resources);
    repository.updateAnnoPageId(annoPages);
  }
}
