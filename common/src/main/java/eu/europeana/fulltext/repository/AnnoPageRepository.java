package eu.europeana.fulltext.repository;

import dev.morphia.AdvancedDatastore;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.entity.AnnoPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;


/**
 * Created by luthien on 31/05/2018.
 */
@Repository
public class AnnoPageRepository {


    @Autowired
    private AdvancedDatastore datastore;

    /**
     * @return the total number of resources in the database
     */
    public long count() {
       return datastore.createQuery(AnnoPage.class).count();
    }

    /**
     * Check if an AnnoPage exists that contains an Annotation that matches the given parameters
     * using DBCollection.count(). In ticket EA-1464 this method was tested as the best performing.
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param pageId    index (page number) of the Annopage object within its parent
     * @return true if yes, otherwise false
     */
    public boolean existsByPageId(String datasetId, String localId, String pageId) {
        return datastore.createQuery(AnnoPage.class)
                 .field("dsId").equal(datasetId)
                 .field("lcId").equal(localId)
                 .field("pgId").equal(pageId).count() >= 1;
    }

    /**
     * Check if an AnnoPage exists that contains an Annotation that matches the given parameters
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param annoId    ID of the annotation
     * @return true if yes, otherwise false
     */
    public boolean existsWithAnnoId(String datasetId, String localId, String annoId) {
        return datastore.createQuery(AnnoPage.class)
                        .field("dsId").equal(datasetId)
                        .field("lcId").equal(localId)
                        .field("ans.anId").equal(annoId).count() >= 1;
    }

    /**
     * Find and return an AnnoPage that matches the given parameters
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param pageId    index (page number) of the Annopage object within its parent
     * @return AnnoPage
     */
    public AnnoPage findByDatasetLocalPageId(String datasetId, String localId, String pageId) {
        return datastore.createQuery(AnnoPage.class)
                        .field("dsId").equal(datasetId)
                        .field("lcId").equal(localId)
                        .field("pgId").equal(pageId).first();
    }

    /**
     * Find and return AnnoPage that contains an annotation that matches the given parameters
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param annoId    ID of the annotation
     * @return AnnoPage
     */
    public AnnoPage findByDatasetLocalAnnoId(String datasetId, String localId, String annoId) {
        return datastore.createQuery(AnnoPage.class)
                        .field("dsId").equal(datasetId)
                        .field("lcId").equal(localId)
                        .field("ans.anId").equal(annoId).first();
    }

    /**
     * Find and return AnnoPage that contains an annotation that matches the given parameters
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param imageId   ID of the image
     * @param textGranularity type of annotations that should be retrieve, if null or empty all annotations of that
     *                        annopage will be retrieved
     * @return AnnoPage
     */
    public AnnoPage findByDatasetLocalImageId(String datasetId, String localId, String imageId, AnnotationType textGranularity) {
        // TODO filter by textGranularity
        return datastore.createQuery(AnnoPage.class)
                .field("dsId").equal(datasetId)
                .field("lcId").equal(localId)
                .field("tgtId").equal(imageId).first();
    }

    /**
     * Deletes all annotation pages part of a particular dataset
     * @param datasetId ID of the dataset to be deleted
     * @return the number of deleted annotation pages
     */
    // TODO move this to the loader?
    public int deleteDataset(String datasetId) {
        return datastore.delete(datastore.createQuery(AnnoPage.class).field("dsId").equal(datasetId)).getN();
    }

    // TODO move this to the loader?
    public void save(AnnoPage apToSave){
        datastore.save(apToSave);
    }

}
