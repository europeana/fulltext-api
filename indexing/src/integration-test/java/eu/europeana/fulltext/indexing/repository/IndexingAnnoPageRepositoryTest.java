package eu.europeana.fulltext.indexing.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.morphia.query.MorphiaCursor;
import eu.europeana.fulltext.indexing.model.AnnoPageRecordId;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Excluded from automated runs")
public class IndexingAnnoPageRepositoryTest {

  @Autowired private IndexingAnnoPageRepository repository;

  @Test
  void shouldFetchModifiedAnnoPages() {

    Instant from = ZonedDateTime.of(2019, 9, 20, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant();

    Instant to = Instant.now();

    try (MorphiaCursor<AnnoPageRecordId> cursor =
        repository.getAnnoPageRecordIdByModificationTime(Optional.of(from), to)) {

      while (cursor.hasNext()) {
        System.out.println(cursor.next());
      }
    }
  }
}
