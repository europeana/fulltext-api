package eu.europeana.fulltext.migrations.repository;

import static dev.morphia.query.experimental.filters.Filters.eq;
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
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.Filters;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.exception.DatabaseQueryException;
import eu.europeana.fulltext.migrations.config.MigrationAppSettings;
import eu.europeana.fulltext.migrations.model.MigrationJobMetadata;
import eu.europeana.fulltext.repository.AnnoPageRepository;
import eu.europeana.fulltext.util.MorphiaUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class MigrationRepository {

  private final Datastore datastore;

  private final int tooManyAnnotationsThreshold;
  private static final Logger logger = LogManager.getLogger(MigrationRepository.class);

  public MigrationRepository(Datastore datastore, MigrationAppSettings settings) {
    this.datastore = datastore;
    this.tooManyAnnotationsThreshold = settings.getTooManyAnnotationsThreshold();
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
  public void upsertAnnoPages(List<? extends AnnoPage> annoPageList) throws DatabaseQueryException {

    List<WriteModel<AnnoPage>> annoPageUpdates = new ArrayList<>();

    Instant now = Instant.now();

    List<AnnoPage> annoPagesWithManyAnnotations = new ArrayList<>();

    for (AnnoPage annoPage : annoPageList) {

      Resource res = annoPage.getRes();
      boolean hasManyAnnotations = annoPage.getAns().size() > this.tooManyAnnotationsThreshold;

      if (res == null) {
        // all AnnoPages should have a resource
        throw new DatabaseQueryException("res is null for " + annoPage);
      }

      Document updateDoc =
          new Document(DATASET_ID, annoPage.getDsId())
              .append(LOCAL_ID, annoPage.getLcId())
              .append(PAGE_ID, annoPage.getPgId())
              .append(TARGET_ID, annoPage.getTgtId())
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

      // AnnoPages with a large number of Annotations need to be updated differently, otherwise
      // Mongo complains about update payload size
      if (!hasManyAnnotations) {
        updateDoc.append(ANNOTATIONS, annoPage.getAns());
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(
              "AnnoPage {} has {} annotations; these will be updated separately; threshold is {}",
              annoPage.getDbId(),
              annoPage.getAns().size(),
              tooManyAnnotationsThreshold);
        }
        annoPagesWithManyAnnotations.add(annoPage);
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

    // Try updating annotations first, so "modified" time isn't changed. In case update fails
    updateAnnotations(annoPagesWithManyAnnotations);
    datastore.getMapper().getCollection(AnnoPage.class).bulkWrite(annoPageUpdates);

  }

  /**
   * For AnnoPages whose annotations exceed the threshold, we update the AnnotationIds separately.
   * Annotation updates are chunked
   *
   * <p>This method creates a list of UpdateOneModels as follows:
   *
   * <pre>
   * {
   *    updateOne: {
   *      "filter":  { _id: <AnnoPage_ObjectId>},
   *      "update": { "$set": { "ans.$[elem].anId": <new_AnnotationID> }},
   *      "arrayFilters": [{ "elem.anId": <existing_AnnotationID> }]
   *   }
   * }
   * </pre>
   *
   * @param annoPagesWithManyAnnotations
   */
  private void updateAnnotations(List<AnnoPage> annoPagesWithManyAnnotations) {
    for (AnnoPage annoPage : annoPagesWithManyAnnotations) {
      AtomicInteger counter = new AtomicInteger();
      Stream<Annotation> annotationStream = annoPage.getAns().stream();
      Document filter = new Document(DOC_ID, annoPage.getDbId());

      // break up Annotation list in chunks
      Collection<List<Annotation>> annotationChunks =
          annotationStream
              .collect(
                  Collectors.groupingBy(
                      it -> counter.getAndIncrement() / tooManyAnnotationsThreshold))
              .values();

      int chunkCount = 1;
      // Write each chunk of Annotation updates separately
      for (List<Annotation> annotations : annotationChunks) {
        List<UpdateOneModel<AnnoPage>> annotationUpdate =
            annotations.stream()
                .map(
                    annotation ->
                        new UpdateOneModel<AnnoPage>(
                            filter,
                            new Document(
                                SET, new Document("ans.$[elem].anId", annotation.getAnId())),
                            new UpdateOptions()
                                .arrayFilter(eq("elem.anId", annotation.getOldAnId()))))
                .collect(Collectors.toList());

        datastore.getMapper().getCollection(AnnoPage.class).bulkWrite(annotationUpdate);

        if (logger.isDebugEnabled()) {
          logger.debug(
              "Updated annotations for AnnoPage {}; chunk {} of {}, chunkSize={}, annotations={} ",
              annoPage.getDbId(),
              chunkCount,
              annotationChunks.size(),
              annotations.size(),
              annoPage.getAns().size());
        }
        chunkCount++;
      }
    }
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
