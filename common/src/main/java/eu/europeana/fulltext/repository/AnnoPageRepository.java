package eu.europeana.fulltext.repository;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Unwind;
import dev.morphia.query.internal.MorphiaCursor;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.entity.AnnoPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.first;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.stages.Group.id;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.in;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.*;
import static eu.europeana.fulltext.util.MorphiaUtils.MULTI_DELETE_OPTS;


/**
 * Created by luthien on 31/05/2018.
 */
@Repository
public class AnnoPageRepository {


    @Autowired
    private Datastore datastore;

    /**
     * @return the total number of resources in the database
     */
    public long count() {
       return datastore.find(AnnoPage.class).count();
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
        return datastore.find(AnnoPage.class).filter(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(PAGE_ID, pageId)
        ).count() > 0 ;
    }

    /**
     * Check if an AnnoPage exists that contains an Annotation that matches the given parameters
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param annoId    ID of the annotation
     * @return true if yes, otherwise false
     */
    public boolean existsWithAnnoId(String datasetId, String localId, String annoId) {
        return datastore.find(AnnoPage.class)
                .filter(
                        eq(DATASET_ID, datasetId),
                        eq(LOCAL_ID, localId),
                        eq(ANNOTATIONS_ID, annoId)
                )
                .count() > 0;
    }

    /**
     * Find and return an AnnoPage that matches the given parameters.
     * Only annotations that match the specified text granularity values are retrieved from the data store.
     * <p>
     * The mongodb query implemented by this method is:
     * db.getCollection("AnnoPage").aggregate(
     * {$match: {"dsId": <datasetId>, "lcId": <localId>, "pgId": <pageId>}},
     * {$unwind: "$ans"},
     * {$match: {"ans.dcType":
     * {"$in": [<textGranValues>]}}},
     * { "$group": {
     * "_id": "$_id",
     * "dsId": { "$first": "$dsId"},
     * "lcId": { "$first": "$lcId"},
     * "pgId":{ "$first": "$pgId"},
     * "tgtId": { "$first": "$tgtId"},
     * "res": { "$first": "$res"},
     * "className": { "$first": "$className"},
     * "ans": { "$push": "$ans" }
     * }}
     * )
     *
     * @param datasetId      ID of the dataset
     * @param localId        ID of the parent of the Annopage object
     * @param pageId         index (page number) of the Annopage object within its parent
     * @param textGranValues dcType values to filter annotations with
     * @return AnnoPage
     */
    public AnnoPage findByDatasetLocalPageId(String datasetId, String localId, String pageId, List<String> textGranValues) {
        Aggregation<AnnoPage> query = datastore.aggregate(AnnoPage.class).match(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(PAGE_ID, pageId)
        );

        if (!textGranValues.isEmpty()) {
            // ans.dcType stored as first letter of text granularity value in uppercase. ie. WORD -> 'W'
            List<String> dcTypes = textGranValues.stream().map(s -> s.substring(0, 1).toUpperCase()).collect(Collectors.toUnmodifiableList());
            query = filterTextGranularity(query, dcTypes);
        }

        return query.execute(AnnoPage.class).tryNext();
    }


    /**
     * Find and return AnnoPage that contains an annotation that matches the given parameters
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param annoId    ID of the annotation
     * @return AnnoPage
     */
    public AnnoPage findByDatasetLocalAnnoId(String datasetId, String localId, String annoId) {
        return datastore.find(AnnoPage.class).filter(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(ANNOTATIONS_ID, annoId))
                .first();
    }

    /**
     * Find and return AnnoPages that contains an annotation that matches the given parameters.
     *
     * Returns a {@link MorphiaCursor} that can be iterated on to obtain matching AnnoPages.
     * The cursor must be closed after iteration is completed.
     *
     * The Cursor returned by this method must be closed
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param imageIds   ID of the image
     * @param textGranularity type of annotations that should be retrieve, if null or empty all annotations of that
     *                        annopage will be retrieved
     * @return MorphiaCursor containing AnnoPage entries.
     */
    public MorphiaCursor<AnnoPage> findByDatasetLocalImageId(String datasetId, String localId, List<String> imageIds, AnnotationType textGranularity) {
        Aggregation<AnnoPage> query = datastore.aggregate(AnnoPage.class).match(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                in(IMAGE_ID, imageIds)
        );

        if (textGranularity != null) {
            query = filterTextGranularity(query, Collections.singletonList(String.valueOf(textGranularity.getAbbreviation())));
        }

        return query.execute(AnnoPage.class);
    }

    /**
     * Deletes all annotation pages part of a particular dataset
     * @param datasetId ID of the dataset to be deleted
     * @return the number of deleted annotation pages
     */
    // TODO move this to the loader?
    public long deleteDataset(String datasetId) {
        return datastore.find(AnnoPage.class).filter(
                eq(DATASET_ID,datasetId))
                .delete(MULTI_DELETE_OPTS).getDeletedCount();
    }

    // TODO move this to the loader?
    public void save(AnnoPage apToSave){
        datastore.save(apToSave);
    }


    /**
     * Creates an AnnoPage aggregation query to return only matching annotation types.
     * @param annoPageQuery aggregation query
     * @param textGranValues list containing text granularity values to match
     * @return Updated aggregation query
     */
    private Aggregation<AnnoPage> filterTextGranularity(Aggregation<AnnoPage> annoPageQuery, List<String> textGranValues) {
        return annoPageQuery.unwind(Unwind.on(ANNOTATIONS)).match(in(ANNOTATIONS_DCTYPE, textGranValues))
                .group(Group.of(id(DOC_ID))
                        .field(ANNOTATIONS, push().single(field(ANNOTATIONS)))
                        .field(DATASET_ID, first(field(DATASET_ID)))
                        .field(LOCAL_ID, first(field(LOCAL_ID)))
                        .field(PAGE_ID, first(field(PAGE_ID)))
                        .field(RESOURCE, first(field(RESOURCE)))
                        .field(CLASSNAME, first(field(CLASSNAME)))
                        .field(IMAGE_ID, first(field(IMAGE_ID)))
                );
    }
}
