package eu.europeana.fulltext.repository;

import dev.morphia.Datastore;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.entity.TranslationResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.*;


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
        LOG.warn("Repository count is temporarily disabled because of bad performance with large collections");
        //return datastore.createQuery(clazz).count();
        return 0;
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

}
