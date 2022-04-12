package eu.europeana.fulltext.repository;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import dev.morphia.Datastore;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltext.entity.TranslationResource;
import eu.europeana.fulltext.exception.DatabaseQueryException;
import eu.europeana.fulltext.util.MorphiaUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Logger LOG = LogManager.getLogger(ResourceRepository.class);

    @Autowired
    protected Datastore datastore;

    // TODO investigate if we can query for both original and translation annopages in 1 query (e.g. with aggregation)
    // If not we could try and sent the original and translation query simultaneously (see also FTService)

    /**
     * Check if an original Resource exists that matches the given parameters
     * @param datasetId ID of the associated dataset
     * @param localId   ID of the associated Annopage parent object
     * @param resId     ID of the Resource document
     * @return true if yes, otherwise false
     */
    public boolean existsOriginal(String datasetId, String localId, String resId) {
        return existsResource(datasetId, localId, resId, Resource.class);
    }


    /**
     * Check if any TranslationResources exist that match the given parameters using DBCollection.count().
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the TranslationAnnopage object
     * @param resId     ID of the Resource document
     * @return true if yes, otherwise false
     */
    public boolean existsTranslation(String datasetId, String localId, String resId) {
        return existsResource(datasetId, localId, resId, TranslationResource.class);
    }

    private boolean existsResource(String datasetId, String localId, String resId, Class clazz) {
        return datastore.find(clazz)
                .filter(
                        eq(DATASET_ID, datasetId),
                        eq(LOCAL_ID, localId),
                        eq(DOC_ID, resId))
                .count() > 0;
    }

    /**
     * @return the total number of original resources in the database
     */
    public long countOriginal() {
        return count(Resource.class);
    }

    /**
     * @return the total number of translation resources in the database
     */
    public long countTranslation() {
        return count(TranslationResource.class);
    }

  private long count(Class clazz) {
    return datastore.getMapper().getCollection(clazz).countDocuments();
  }

    /**
     * Find an original Resource that matches the given parameters
     * @param datasetId ID of the associated dataset
     * @param localId   ID of the associated Annopage parent object
     * @param resId     ID of the Resource document
     * @return List containing matching Resource(s) (should be just one)
     */
    public Resource findOriginalByResId(String datasetId, String localId, String resId) {
        return datastore.find(Resource.class)
                .filter(
                        eq(DATASET_ID, datasetId),
                        eq(LOCAL_ID, localId),
                        eq(DOC_ID, resId))
                .first();
    }

    /**
     * Find a Translation Resource that matches the given parameters
     * @param datasetId ID of the associated dataset
     * @param localId   ID of the associated TranslationAnnopage parent object
     * @param resId     ID of the Resource document
     * @return List containing matching Resource(s) (should be just one)
     */
    public TranslationResource findTranslationByResId(String datasetId, String localId, String resId) {
        return datastore.find(TranslationResource.class)
                .filter(
                        eq(DATASET_ID, datasetId),
                        eq(LOCAL_ID, localId),
                        eq(DOC_ID, resId))
                .first();
    }

    /**
     * Saves a Resource to the database
     *
     * @param resource Translation Resource object to save
     * @return the saved resource document
     */
    public TranslationResource saveResource(TranslationResource resource) {
        return datastore.save(resource);
    }

    public long deleteResources(String datasetId, String localId) {
        return datastore
            .find(TranslationResource.class)
            .filter(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId))
            .delete(MorphiaUtils.MULTI_DELETE_OPTS)
            .getDeletedCount();
    }

    public long deleteResource(String datasetId, String localId, String lang) {
        return datastore
            .find(TranslationResource.class)
            .filter(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId), eq(LANGUAGE, lang))
            .delete()
            .getDeletedCount();
    }

    /** Only for tests */
    public void deleteAll() {
        datastore.find(TranslationResource.class).delete(MorphiaUtils.MULTI_DELETE_OPTS);
    }

    public BulkWriteResult upsertFromAnnoPage(List<? extends TranslationAnnoPage> annoPageList)
        throws DatabaseQueryException {
        List<WriteModel<TranslationResource>> resourceUpdates = new ArrayList<>();
        for (TranslationAnnoPage annoPage : annoPageList) {
            TranslationResource res = annoPage.getRes();
            if (res == null) {
                // all AnnoPages should have a resource
                throw new DatabaseQueryException("res is null for " + annoPage);
            }

            resourceUpdates.add(
                new UpdateOneModel<>(
                    new Document(
                        // filter
                        Map.of(
                            DATASET_ID, res.getDsId(), LOCAL_ID, res.getLcId(), LANGUAGE, res.getLang())),
                    // update doc
                    new Document(
                        SET,
                        new Document(DATASET_ID, res.getDsId())
                            .append(LOCAL_ID, res.getLcId())
                            .append(LANGUAGE, res.getLang())
                            .append(VALUE, res.getValue())
                            .append(RIGHTS, res.getRights()))
                        // only create _id for new records
                        .append(SET_ON_INSERT, new Document("_id", res.getId())),
                    UPSERT_OPTS));
        }

        return datastore
            .getMapper()
            .getCollection(TranslationResource.class)
            .bulkWrite(resourceUpdates);
    }
}
