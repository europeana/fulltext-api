package eu.europeana.fulltext.indexing.repository;

import static dev.morphia.query.experimental.filters.Filters.gt;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.DATASET_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.LANGUAGE;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.LOCAL_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.MODIFIED;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.PAGE_ID;

import com.mongodb.client.model.Filters;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import eu.europeana.fulltext.entity.AnnoPage;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class IndexingAnnoPageRepository {

  @Autowired private Datastore datastore;

  public List<AnnoPage> getRecordsModifiedAfter(long date) {
    return datastore
        .find(AnnoPage.class)
        .filter(gt(MODIFIED, date))
        .iterator(
            new FindOptions()
                .projection()
                .include(DATASET_ID, LOCAL_ID, PAGE_ID, LANGUAGE, MODIFIED))
        .toList();
  }
}
