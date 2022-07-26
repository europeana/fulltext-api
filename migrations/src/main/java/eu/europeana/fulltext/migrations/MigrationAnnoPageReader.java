package eu.europeana.fulltext.migrations;

import static eu.europeana.fulltext.util.GeneralUtils.getAnnoPageObjectIds;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.migrations.model.MigrationJobMetadata;
import eu.europeana.fulltext.migrations.repository.MigrationRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.data.AbstractPaginatedDataItemReader;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

public class MigrationAnnoPageReader extends AbstractPaginatedDataItemReader<AnnoPage> {

  private static final Logger logger = LogManager.getLogger(MigrationAnnoPageReader.class);

  protected final int limit;
  protected final MigrationRepository repository;
  protected final MigrationJobMetadata jobMetadata;

  public MigrationAnnoPageReader(
      int limit, MigrationRepository repository, MigrationJobMetadata jobMetadata) {
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
    setName(getClassName());
  }

  @NotNull
  @Override
  protected Iterator<AnnoPage> doPageRead() {
    // calls to this method are synchronized in parent class, so we can update lastObjectId here
    if (logger.isDebugEnabled()) {
      logger.debug("Reading from DB. lastObjectId={}", jobMetadata.getLastAnnoPageId());
    }

    List<AnnoPage> annoPageIds = getAnnoPages(limit, jobMetadata.getLastAnnoPageId());
    if (!CollectionUtils.isEmpty(annoPageIds)) {
      jobMetadata.setLastAnnoPageId(annoPageIds.get(annoPageIds.size() - 1).getDbId());

      if (logger.isDebugEnabled()) {
        logger.debug(
            "Fetched {} records. set new lastObjectId={}; fetched records={}",
            annoPageIds.size(),
            jobMetadata.getLastAnnoPageId(),
            Arrays.toString(getAnnoPageObjectIds(annoPageIds)));
      }

      return annoPageIds.iterator();
    }

    if (logger.isDebugEnabled()) {
      logger.debug(
          "No results found during AnnoPage read. lastObjectId={}",
          jobMetadata.getLastAnnoPageId());
    }
    return Collections.emptyIterator();
  }

  protected String getClassName(){
    return MigrationAnnoPageReader.class.getName();
  }

  protected List<AnnoPage> getAnnoPages(int count, @Nullable ObjectId objectId){
    return repository.getAnnoPages(count, objectId, false);
  }
}
