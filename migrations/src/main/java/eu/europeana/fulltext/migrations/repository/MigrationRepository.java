package eu.europeana.fulltext.migrations.repository;

import static dev.morphia.query.experimental.filters.Filters.exists;
import static dev.morphia.query.experimental.filters.Filters.in;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.*;
import static eu.europeana.fulltext.util.MorphiaUtils.RESOURCE_COL;
import static eu.europeana.fulltext.util.MorphiaUtils.SET;
import static eu.europeana.fulltext.util.MorphiaUtils.UPSERT_OPTS;

import com.mongodb.DBRef;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.Filters;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.exception.DatabaseQueryException;
import eu.europeana.fulltext.migrations.model.MigrationJobMetadata;
import eu.europeana.fulltext.repository.AnnoPageRepository;
import eu.europeana.fulltext.util.MorphiaUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class MigrationRepository {

  private final Datastore datastore;

  public MigrationRepository(Datastore datastore) {
    this.datastore = datastore;
  }

  public List<AnnoPage> getAnnoPages(int count, @Nullable ObjectId objectId) {

    Query<AnnoPage> findQuery = datastore.find(AnnoPage.class);
    if (objectId != null) {
      findQuery.filter(Filters.gt("_id", objectId));
    }

    return findQuery.iterator(new FindOptions().limit(count)).toList();
  }

  /**
   * Similar to @{@link AnnoPageRepository#upsertAnnoPages(List)}, just that this method always
   * updates the Resource reference
   *
   * @param annoPageList list of AnnoPages to upsert
   * @return
   * @throws DatabaseQueryException
   */
  public BulkWriteResult upsertAnnoPages(List<? extends AnnoPage> annoPageList)
      throws DatabaseQueryException {

    List<WriteModel<AnnoPage>> annoPageUpdates = new ArrayList<>();

    Instant now = Instant.now();

    for (AnnoPage annoPage : annoPageList) {

      Resource res = annoPage.getRes();

      if (res == null) {
        // all AnnoPages should have a resource
        throw new DatabaseQueryException("res is null for " + annoPage);
      }

      Document updateDoc =
          new Document(DATASET_ID, annoPage.getDsId())
              .append(LOCAL_ID, annoPage.getLcId())
              .append(PAGE_ID, annoPage.getPgId())
              .append(TARGET_ID, annoPage.getTgtId())
              .append(ANNOTATIONS, annoPage.getAns())
              .append(MODIFIED, now)
              .append(LANGUAGE, annoPage.getLang())
              .append(RESOURCE, new DBRef(RESOURCE_COL, res.getId()));

      // source isn't always set. Prevent null from being saved in db
      if (annoPage.getSource() != null) {
        updateDoc.append(SOURCE, annoPage.getSource());
      }

      annoPageUpdates.add(
          new UpdateOneModel<>(
              new Document(
                  // dbId exists since this AnnoPage was retrieved with Morphia
                  DOC_ID, annoPage.getDbId()),
              new Document(SET, updateDoc),
              UPSERT_OPTS));
    }

    return datastore.getMapper().getCollection(AnnoPage.class).bulkWrite(annoPageUpdates);
  }

  /**
   * Deletes Resources with the given document ids
   *
   * @param resourceIds _ids of Resources to delete
   */
  public void deleteResource(List<String> resourceIds) {
    datastore
        .find(Resource.class)
        .filter(in(DOC_ID, resourceIds))
        .delete(MorphiaUtils.MULTI_DELETE_OPTS);
  }

  public void save(List<Resource> resources) {
    datastore.save(resources);
  }

  public MigrationJobMetadata getExistingMetadata() {
    return datastore
        .find(MigrationJobMetadata.class)
        .filter(exists("lastAnnoPageIdRef"))
        .iterator()
        .tryNext();
  }

  public MigrationJobMetadata save(MigrationJobMetadata jobMetadata){
    return datastore.save(jobMetadata);
  }
}
