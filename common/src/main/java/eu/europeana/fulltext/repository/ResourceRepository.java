package eu.europeana.fulltext.repository;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import dev.morphia.Datastore;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.exception.DatabaseQueryException;
import eu.europeana.fulltext.util.MorphiaUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.in;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.*;
import static eu.europeana.fulltext.util.MorphiaUtils.SET;
import static eu.europeana.fulltext.util.MorphiaUtils.SET_ON_INSERT;
import static eu.europeana.fulltext.util.MorphiaUtils.UPSERT_OPTS;


/**
 * Created by luthien on 31/05/2018.
 */
@Repository
public class ResourceRepository {

    @Value("${spring.profiles.active:}")
    private String activeProfileString;

    @Autowired
    protected Datastore datastore;

    /**
     * @return the total number of resources in the database
     */
    public long count() {
        return datastore.getMapper().getCollection(Resource.class).countDocuments();
    }

    /**
    * Find a resource that matches the specified parameters
    */
    public Resource findByPageIdLang(String datasetId, String localId, String pageId, String lang) {
        return datastore.find(Resource.class)
            .filter(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(PAGE_ID, pageId),
                eq(LANGUAGE, lang))
            .first();
    }

    public Resource findOriginalByPageId(String datasetId, String localId, String pageId) {
        return datastore.find(Resource.class)
            .filter(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(PAGE_ID, pageId),
                eq(TRANSLATION, null))
            .first();
    }


    /**
    * Check if a Resource exists that matches the given parameters
    * @param datasetId ID of the associated dataset
    * @param localId   ID of the associated Annopage parent object
    * @param pageId     ID of the associated AnnoPage page
    * @param lang     language of the associated AnnoPage document
    * @return true if yes, otherwise false
    */
    public boolean resourceExists(String datasetId, String localId, String pageId, String lang) {
        return datastore.find(Resource.class)
            .filter(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(PAGE_ID, pageId),
                eq(LANGUAGE, lang))
            .count() > 0;
    }

    /**
     * Saves a Resource to the database
     *
     * @param resource Resource object to save
     * @return the saved resource document
     */
    public Resource saveResource(Resource resource) {
        return datastore.save(resource);
    }

  /**
   * Deletes the Resource(s) that match the specified parameters
   * @param datasetId Dataset id
   * @param localId Local id
   * @return number of deleted records
   */
    public long deleteResources(String datasetId, String localId) {
        return datastore
            .find(Resource.class)
            .filter(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId))
            .delete(MorphiaUtils.MULTI_DELETE_OPTS)
            .getDeletedCount();
    }

  /**
   *  Deletes the Resource that matches the specified parameters
   * @param datasetId dataset id
   * @param localId local id
   * @param lang Resource language
   * @return number of deleted records. Should be either 1 or 0.
   */
    public long deleteResource(String datasetId, String localId, String lang) {
        return datastore
            .find(Resource.class)
            .filter(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId), eq(LANGUAGE, lang))
            .delete()
            .getDeletedCount();
    }

    /** Only for tests */
    public void deleteAll() {
      MorphiaUtils.validateDeletion(activeProfileString);
      datastore.find(Resource.class).delete(MorphiaUtils.MULTI_DELETE_OPTS);
    }

  public BulkWriteResult upsertFromAnnoPage(List<? extends AnnoPage> annoPageList)
        throws DatabaseQueryException {
        List<WriteModel<Resource>> resourceUpdates = new ArrayList<>();
        for (AnnoPage annoPage : annoPageList) {
            Resource res = annoPage.getRes();
            if (res == null) {
                // all AnnoPages should have a resource
                throw new DatabaseQueryException("res is null for " + annoPage);
            }

            Document updateDoc = new Document(DATASET_ID, res.getDsId())
                    .append(LOCAL_ID, res.getLcId())
                    .append(LANGUAGE, res.getLang())
                    .append(VALUE, res.getValue())
                    .append(RIGHTS, res.getRights())
                    .append(PAGE_ID, res.getPgId())
                    .append(CONTRIBUTED, res.isContributed());

            // don't set translation=false in db, to conserve space
            if (res.isTranslation()) {
                updateDoc.append(TRANSLATION, res.isTranslation());
            }

            resourceUpdates.add(
                new UpdateOneModel<>(
                    new Document(
                        // filter
                        Map.of(
                            DATASET_ID, res.getDsId(), LOCAL_ID, res.getLcId(), LANGUAGE, res.getLang(), DOC_ID, res.getId())),
                        // update doc
                        new Document(
                                SET, updateDoc)
                        // only create _id for new records
                        .append(SET_ON_INSERT, new Document("_id", res.getId())),
                    UPSERT_OPTS));
        }

        return datastore
            .getMapper()
            .getCollection(Resource.class)
            .bulkWrite(resourceUpdates);
    }

  /**
   * Deletes the Resources with _id values matching contents of provided list
   * @param resourceIds Resource _ids to match
   * @return Number of deleted records
   */
  public long deleteResourcesById(List<String> resourceIds) {
      return datastore
          .find(Resource.class)
          .filter(in(DOC_ID, resourceIds))
          .delete(MorphiaUtils.MULTI_DELETE_OPTS)
          .getDeletedCount();
  }
}
