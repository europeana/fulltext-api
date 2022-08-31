package eu.europeana.fulltext.indexing.reader;

import dev.morphia.query.MorphiaCursor;
import eu.europeana.fulltext.indexing.model.AnnoPageRecordId;
import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.data.AbstractPaginatedDataItemReader;

/**
 * Reader for fetching recordIds (dsId + lcId combination) of recently-modified
 * AnnoPages
 */
public class AnnoPageRecordIdReader extends AbstractPaginatedDataItemReader<AnnoPageRecordId> {
  private static final Logger logger = LogManager.getLogger(AnnoPageRecordIdReader.class);
  private final IndexingAnnoPageRepository repository;
  private final Instant from;
  private final Instant to;

  private MorphiaCursor<AnnoPageRecordId> cursor;

  public AnnoPageRecordIdReader(IndexingAnnoPageRepository repository, Instant from, Instant to) {
    this.repository = repository;
    this.from = from;
    this.to = to;
  }

  @NotNull
  @Override
  protected Iterator<AnnoPageRecordId> doPageRead() {
    if (logger.isTraceEnabled()) {
      logger.trace("Reading Item {} from cursor", page);
    }

    if (cursor != null && cursor.hasNext()){
      // Note: cursor.next reuses the same object reference, so we have to copy the values
      AnnoPageRecordId next = cursor.next();
      if (logger.isTraceEnabled()) {
        logger.trace("Retrieved {}", next);
      }

      return List.of(next.copy()).iterator();
    }

    // the call to this method is synchronized, see AbstractPaginatedDataItemReader.doRead()
    return Collections.emptyIterator();
  }

  @Override
  protected void doOpen() throws Exception {
    super.doOpen();
    // Non-restartable, as we expect this to run in multi-threaded steps.
    // see: https://stackoverflow.com/a/20002493
    setSaveState(false);
    setName(AnnoPageRecordIdReader.class.getName());

    if (cursor == null) {
      cursor = repository.getAnnoPageRecordIdByModificationTime(Optional.ofNullable(from), to);
      if (logger.isDebugEnabled()) {
        logger.debug("Created Mongo Cursor for retrieving AnnoPage recordIds");
      }
    }
  }

  @Override
  protected void doClose() throws Exception {
    super.doClose();
    if (cursor != null) {
      cursor.close();
      if (logger.isDebugEnabled()) {
        logger.debug("Closed Mongo cursor");
      }
    }
  }
}
