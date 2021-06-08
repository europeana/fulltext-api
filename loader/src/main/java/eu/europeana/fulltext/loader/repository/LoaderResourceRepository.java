package eu.europeana.fulltext.loader.repository;

import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.entity.TranslationResource;
import eu.europeana.fulltext.repository.ResourceRepository;
import org.springframework.stereotype.Repository;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.DATASET_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.MULTI_DELETE_OPTS;


/**
 * Created by luthien on 31/05/2018.
 */
@Repository
public class LoaderResourceRepository extends ResourceRepository {

    /**
     * Deletes all resources associated with a particular dataset
     * @param datasetId ID of the associated dataset
     * @return the number of deleted resources
     */
    public long deleteOriginalDataset(String datasetId) {
        return deleteDataset(datasetId, Resource.class);
    }

    public long deleteTranslationDataset(String datasetId) {
        return deleteDataset(datasetId, TranslationResource.class);
    }

    private long deleteDataset(String datasetId, Class clazz) {
        return datastore.find(clazz)
                .filter(eq(DATASET_ID, datasetId)).delete(MULTI_DELETE_OPTS).getDeletedCount();
    }

    public void saveOriginal(Resource resToSave){
        datastore.save(resToSave);
    }

    public void saveTranslation(TranslationResource resToSave){
        datastore.save(resToSave);
    }
}
