package eu.europeana.fulltext.indexing.repository;

import static dev.morphia.query.experimental.filters.Filters.*;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.*;

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

  //TODO API TEAM: this query is really slow, any way to speed it up (e.g., indexing the field 'modified')?
  public List<AnnoPage> getRecordsModifiedAfter(long date) {
    return datastore
        .find(AnnoPage.class)
        .filter(gt(MODIFIED, date))
        .iterator(
            new FindOptions()
                .projection()
                .include(DATASET_ID, LOCAL_ID))
        .toList();
  }

  public List<AnnoPage> getAllWebResources(String dsId, String lcId) {
    return datastore
            .find(AnnoPage.class)
            .filter(and(eq(DATASET_ID, dsId),eq(LOCAL_ID,lcId)))
            .iterator(
                    new FindOptions()
                            .projection()
                            .include(DATASET_ID, LOCAL_ID, TARGET_ID, LANGUAGE, MODIFIED, RESOURCE))
            .toList();
  }

  public boolean isDeleted(String dsId, String lcId) {
    //TODO API TEAM: Query + logic to determine if all resources associated to same dsId and lcId are deleted
    return false;
  }
}
