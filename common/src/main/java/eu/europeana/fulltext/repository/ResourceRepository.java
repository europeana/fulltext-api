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
     * Check if a Resource exists that matches the given parameters
     * @param datasetId ID of the associated dataset
     * @param localId   ID of the associated Annopage parent object
     * @param resId     ID of the Resource document
     * @return true if yes, otherwise false
     */
    public boolean resourceExists(String datasetId, String localId, String resId) {
      return datastore.find(Resource.class)
              .filter(
                      eq(DATASET_ID, datasetId),
                      eq(LOCAL_ID, localId),
                      eq(DOC_ID, resId))
              .count() > 0;
    }


  /**
     * @return the total number of resources in the database
     */
  public long count() {
    return datastore.getMapper().getCollection(Resource.class).countDocuments();
  }

    /**
     * Find a Resource that matches the given parameters
     * @param datasetId ID of the associated dataset
     * @param localId   ID of the associated Annopage parent object
     * @param resId     ID of the Resource document
     * @return List containing matching Resource(s) (should be just one)
     */
    public Resource findByResId(String datasetId, String localId, String resId) {
        return datastore.find(Resource.class)
                .filter(
                        eq(DATASET_ID, datasetId),
                        eq(LOCAL_ID, localId),
                        eq(DOC_ID, resId))
                .first();
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

    public long deleteResources(String datasetId, String localId) {
        return datastore
            .find(Resource.class)
            .filter(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId))
            .delete(MorphiaUtils.MULTI_DELETE_OPTS)
            .getDeletedCount();
    }

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

            resourceUpdates.add(
                new UpdateOneModel<>(
                    new Document(
                        // filter
                        Map.of(
                            DATASET_ID, res.getDsId(), LOCAL_ID, res.getLcId(), LANGUAGE, res.getLang(), DOC_ID, res.getId())),
                    // update doc
                    new Document(
                        SET,
                        new Document(DATASET_ID, res.getDsId())
                            .append(LOCAL_ID, res.getLcId())
                            .append(LANGUAGE, res.getLang())
                            .append(VALUE, res.getValue())
                            .append(RIGHTS, res.getRights())
                            .append(CONTRIBUTED, res.isContributed())
                    )
                        // only create _id for new records
                        .append(SET_ON_INSERT, new Document("_id", res.getId())),
                    UPSERT_OPTS));
        }

        return datastore
            .getMapper()
            .getCollection(Resource.class)
            .bulkWrite(resourceUpdates);
    }
}
