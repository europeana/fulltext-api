package eu.europeana.fulltext.migrations;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.migrations.repository.MigrationRepository;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.data.AbstractPaginatedDataItemReader;

public class MigrationAnnoPageReader extends AbstractPaginatedDataItemReader<AnnoPage> {

  private final int limit;
  private final MigrationRepository repository;
  private final MigrationJobMetadata jobMetadata;

  public MigrationAnnoPageReader(int limit, MigrationRepository repository,
      MigrationJobMetadata jobMetadata) {
    this.repository = repository;
    this.limit = limit;
    this.jobMetadata = jobMetadata;
  }

  @Override
  protected void doOpen() throws Exception {
    super.doOpen();
    // Non-restartable, as we expect this to run in multi-threaded steps.
    // see: https://stackoverflow.com/a/20002493
    setSaveState(false);
    setName(MigrationAnnoPageReader.class.getName());
  }

  @NotNull
  @Override
  protected Iterator<AnnoPage> doPageRead() {
    // calls to this method are synchronized in parent class, so we can update lastObjectId here
    List<AnnoPage> annoPageIds = repository.getAnnoPages(limit, jobMetadata.getLastAnnoPageId());

    if (annoPageIds != null) {
      jobMetadata.setLastAnnoPageId(annoPageIds.get(annoPageIds.size() - 1).getDbId());
      return annoPageIds.iterator();
    }
    return Collections.emptyIterator();
  }
}
