package eu.europeana.fulltext.migrations.repository;

import static dev.morphia.query.experimental.filters.Filters.exists;
import static dev.morphia.query.experimental.filters.Filters.in;
import static dev.morphia.query.experimental.filters.Filters.lt;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.*;
import static eu.europeana.fulltext.util.MorphiaUtils.RESOURCE_COL;
import static eu.europeana.fulltext.util.MorphiaUtils.SET;
import static eu.europeana.fulltext.util.MorphiaUtils.SET_ON_INSERT;
import static eu.europeana.fulltext.util.MorphiaUtils.UNSET;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
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

  public List<AnnoPage> getAnnoPages(
      int count, @Nullable ObjectId objectId, boolean useProjection) {

    Query<AnnoPage> findQuery = datastore.find(AnnoPage.class);
    if (objectId != null) {
      findQuery.filter(Filters.gt("_id", objectId));
    }

    FindOptions findOpts = new FindOptions().limit(count);

    if (useProjection) {
      findOpts.projection().include(TARGET_ID, RESOURCE);
    }

    return findQuery.iterator(findOpts).toList();
  }

  /**
   * Get AnnoPages modified before the given date
   *
   * @return
   */
  public List<AnnoPage> getAnnoPagesModifiedBefore(Date date, int skip, int limit) {
    return datastore
        .find(AnnoPage.class)
        .filter(lt(MODIFIED, date))
        .iterator(new FindOptions().skip(skip).limit(limit))
        .toList();
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

      // don't set translation=false in db, to conserve space
      if (annoPage.isTranslation()) {
        updateDoc.append(TRANSLATION, annoPage.isTranslation());
      }

      annoPageUpdates.add(
          new UpdateOneModel<>(
              new Document(
                  // dbId exists since this AnnoPage was retrieved with Morphia
                  DOC_ID, annoPage.getDbId()),
              new Document(SET, updateDoc)
                  // remove Morphia discriminator
                  .append(UNSET, new Document(CLASSNAME, "")),
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

  /**
   * Saves the Resources in the list. Uses a BulkWrite query instead of datastore.save() as we don't
   * want to save with the Morphia discriminator.
   *
   * @param resources list of resources to save
   */
  public void save(List<Resource> resources) {
    List<WriteModel<Resource>> resourceUpdates = new ArrayList<>();
    for (Resource res : resources) {
      Document updateDoc =
          new Document(DATASET_ID, res.getDsId())
              .append(LOCAL_ID, res.getLcId())
              .append(LANGUAGE, res.getLang())
              .append(VALUE, res.getValue())
              .append(RIGHTS, res.getRights())
              .append(PAGE_ID, res.getPgId());

      // don't set translation=false in db, to conserve space
      if (res.isTranslation()) {
        updateDoc.append(TRANSLATION, res.isTranslation());
      }
      resourceUpdates.add(
          new UpdateOneModel<>(
              new Document(
                  // filter
                  Map.of(
                      DATASET_ID,
                      res.getDsId(),
                      LOCAL_ID,
                      res.getLcId(),
                      LANGUAGE,
                      res.getLang(),
                      DOC_ID,
                      res.getId())),
              // update doc
              new Document(SET, updateDoc)
                  // only create _id for new records
                  .append(SET_ON_INSERT, new Document("_id", res.getId())),
              UPSERT_OPTS));
    }

    datastore.getMapper().getCollection(Resource.class).bulkWrite(resourceUpdates);
  }

  public MigrationJobMetadata getExistingMetadata() {
    return datastore
        .find(MigrationJobMetadata.class)
        .filter(exists("lastAnnoPageIdRef"))
        .iterator()
        .tryNext();
  }

  public void save(MigrationJobMetadata jobMetadata) {
    datastore.save(jobMetadata);
  }

  public void updateResourcePgId(List<Resource> resources) {
    List<WriteModel<Resource>> resourceUpdates = new ArrayList<>();

    for (Resource res : resources) {
      resourceUpdates.add(
          new UpdateOneModel<>(
              new Document(
                  // filter
                  Map.of(DOC_ID, res.getId())),
              // update doc
              new Document(SET, new Document(PAGE_ID, res.getPgId()))));
    }
    datastore.getMapper().getCollection(Resource.class).bulkWrite(resourceUpdates);
  }

  public void updateAnnoPageId(List<? extends AnnoPage> annoPages) {
    List<WriteModel<AnnoPage>> annoPageUpdates = new ArrayList<>();

    for (AnnoPage annoPage : annoPages) {
      annoPageUpdates.add(
          new UpdateOneModel<>(
              new Document(
                  // filter
                  Map.of(DOC_ID, annoPage.getDbId())),
              // update doc
              new Document(SET, new Document(PAGE_ID, annoPage.getPgId()))));
    }
    datastore.getMapper().getCollection(AnnoPage.class).bulkWrite(annoPageUpdates);
  }
}
