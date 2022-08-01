package eu.europeana.fulltext.migrations;

import static eu.europeana.fulltext.util.GeneralUtils.getAnnoPageObjectIds;
import static java.time.LocalDate.of;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.migrations.repository.MigrationRepository;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.data.AbstractPaginatedDataItemReader;

public class MigrationAnnoPageModificationReader extends AbstractPaginatedDataItemReader<AnnoPage> {

  private static final Logger logger =
      LogManager.getLogger(MigrationAnnoPageModificationReader.class);
  private final MigrationRepository repository;

  // hard-coded date before the start of the migration run
    private final Date maxModificationDate =
        Date.from(of(2022, Month.JULY,
   1).atStartOfDay(ZoneId.of("UTC+1")).toInstant());

  private final int readerPageSize;

  public MigrationAnnoPageModificationReader(
       int readerPageSize, MigrationRepository repository) {
    this.repository = repository;
    this.readerPageSize = readerPageSize;
  }

  @Override
  protected void doOpen() throws Exception {
    super.doOpen();
    setSaveState(false);
    setPageSize(readerPageSize);
    setName(MigrationAnnoPageModificationReader.class.getName());
  }

  @NotNull
  @Override
  protected Iterator<AnnoPage> doPageRead() {
    int skip = page * pageSize;

    List<AnnoPage> records = repository.getAnnoPagesModifiedBefore(maxModificationDate, skip, pageSize);

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Fetched {} records. skip={}, pageSize={}, annoPageIds={}",
          records.size(),
          skip,
          pageSize,
          Arrays.toString(getAnnoPageObjectIds(records)));
    }
    return records.iterator();
  }
}
