package eu.europeana.fulltext.repository;

import eu.europeana.fulltext.entity.Resource;
import dev.morphia.AdvancedDatastore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;


/**
 * Created by luthien on 31/05/2018.
 */
@Repository
public class ResourceRepository {


    @Autowired
    private AdvancedDatastore datastore;

    /**
     * Check if a Resource exists that matches the given parameters
     * @param datasetId ID of the associated dataset
     * @param localId   ID of the associated Annopage parent object
     * @param resId     ID of the Resource document
     * @return true if yes, otherwise false
     */
    public boolean existsByLimitOne(String datasetId, String localId, String resId) {
        return datastore.createQuery(Resource.class)
                                .field("dsId").equal(datasetId)
                                .field("lcId").equal(localId)
                                .field("_id").equal(resId).count() >= 1;
    }

    /**
     * @return the total number of resources in the database
     */
    public long count() {
        return datastore.createQuery(Resource.class).count();
    }

    /**
     * Find a Resource that matches the given parameters
     * @param datasetId ID of the associated dataset
     * @param localId   ID of the associated Annopage parent object
     * @param resId     ID of the Resource document
     * @return List containing matching Resource(s) (should be just one)
     */
    public Resource findByDatasetLocalResId(String datasetId, String localId, String resId) {
        return datastore.createQuery(Resource.class)
                        .field("dsId").equal(datasetId)
                        .field("lcId").equal(localId)
                        .field("_id").equal(resId).first();
    }

/**
     * Deletes all resources associated with a particular dataset
     * @param datasetId ID of the associated dataset
     * @return the number of deleted resources
 */
    public int deleteDataset(String datasetId) {
        return datastore.delete(datastore.createQuery(Resource.class).field("dsId").equal(datasetId)).getN();
    }

    public void save(Resource resToSave){
        datastore.save(resToSave);
    }
}
