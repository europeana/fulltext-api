package eu.europeana.fulltext.loader.repository;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.repository.AnnoPageRepository;
import org.springframework.stereotype.Repository;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.DATASET_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.MULTI_DELETE_OPTS;


/**
 * Repository for retrieving AnnoPage objects / data
 * Created by luthien on 31/05/2018.
 * @deprecated since 2023
 */
@Deprecated
@Repository
public class LoaderAnnoPageRepository extends AnnoPageRepository {

    /**
     * Deletes all annotation pages part of a particular dataset
     * @param datasetId ID of the dataset to be deleted
     * @return the number of deleted annotation pages
     */
    public long deleteDataset(String datasetId) {
        return datastore.find(Resource.class).filter(
                eq(DATASET_ID, datasetId))
                .delete(MULTI_DELETE_OPTS).getDeletedCount();
    }


    public void save(AnnoPage apToSave){
        datastore.save(apToSave);
    }

}
