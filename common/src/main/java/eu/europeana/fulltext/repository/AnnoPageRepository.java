package eu.europeana.fulltext.repository;
import eu.europeana.fulltext.entity.AnnoPage;
import dev.morphia.AdvancedDatastore;
import dev.morphia.query.Query;
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
//    @ExistsQuery("{'dsId':'?0', 'lcId':'?1', 'ans.anId':'?2'}")
    public boolean existsWithAnnoId(String datasetId, String localId, String annoId) {
        return datastore.createQuery(AnnoPage.class)
                        .field("dsId").equal(datasetId)
                        .field("lcId").equal(localId)
                        .field("ans.anId").equal(annoId).count() >= 1;
    }
//    public boolean existsWithAnnoId(String datasetId, String localId, String annoId) {
//        Query<AnnoPage> query = datastore.createQuery(AnnoPage.class);
//        query.and(
//                query.criteria("dsId").equal(datasetId),
//                query.criteria("lcId").equal(localId),
//                query.criteria("ans.anId").equal(annoId));
//        return (null != query.first());
//    }

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
     * Deletes all annotation pages part of a particular dataset
     * @param datasetId ID of the dataset to be deleted
     * @return the number of deleted annotation pages
     */
    public int deleteDataset(String datasetId) {
        return datastore.delete(datastore.createQuery(AnnoPage.class).field("dsId").equal(datasetId)).getN();
    }

    public void save(AnnoPage apToSave){
        datastore.save(apToSave);
    }

}
