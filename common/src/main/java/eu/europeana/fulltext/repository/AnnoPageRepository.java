package eu.europeana.fulltext.repository;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCursor;
import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.expressions.ArrayExpressions;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.query.internal.MorphiaCursor;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dev.morphia.aggregation.experimental.expressions.ArrayExpressions.filter;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.expressions.Expressions.value;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.in;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.*;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;


/**
 * Repository for retrieving AnnoPage objects / data
 * Created by luthien on 31/05/2018.
 */
@Repository
public class AnnoPageRepository {

    private static final Logger LOG = LogManager.getLogger(AnnoPageRepository.class);

    @Autowired
    protected Datastore datastore;

    // TODO investigate if we can query for both original and translation annopages in 1 query (e.g. with aggregation)
    // If not we could try and sent the original and translation query simultaneously (see also FTService)

    /**
     * @return the total number of original AnnoPages in the database
     */
    public long countOriginal() {
        return count(AnnoPage.class);
    }

    /**
     * @return the total number of TranslationAnnoPages in the database
     */
    public long countTranslation() {
        return count(TranslationAnnoPage.class);
    }

    private long count(Class clazz) {
        // TODO investigate why this query is so slow and fix
        LOG.warn("Repository count is temporarily disabled because of bad performance with large collections");
        //return datastore.createQuery(clazz).count();
        return 0;
    }

    /**
     * Check if any AnnoPages exist that match the given parameters using DBCollection.count().
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @return true if yes, otherwise false
     */
    public long existForEuropeanaId(String datasetId, String localId, Class claph) {
        return datastore.find(claph).filter(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId)
        ).count();
    }

    /**
     * Find and return AnnoPages that match the given parameters.
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @return List of AnnoPage objects
     */
    public List<AnnoPage> findOrigPages(String datasetId, String localId) {
        //TODO instead of loading the AnnoPage + Resource, we should load have the option to only the AnnoPage
        return datastore.find(AnnoPage.class).filter(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId)).iterator().toList();
    }

    /**
     * Find and return single AnnoPage that match the given parameters.
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @return AnnoPage
     */
    public AnnoPage findPage(String datasetId, String localId) {
        return datastore.find(AnnoPage.class).filter(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId)).first();
    }

    /**
     * Find and return TranslationAnnoPages that match the given parameters using DBCollection.count().
     * The Morphia ReferenceException is thrown if there is no matching TranslationResource document found;
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the TranslationAnnopage object
     * @param pageId    index (page number) of the TranslationAnnopage object
     * @return List of TranslationAnnopage objects
     */
    public List<TranslationAnnoPage> findTranslatedPages(String datasetId, String localId, String pageId) {
        // TODO instead of loading the AnnoPage + Resource, we should load only the AnnoPage
        return datastore.find(TranslationAnnoPage.class).filter(
                    eq(DATASET_ID, datasetId),
                    eq(LOCAL_ID, localId),
                    eq(PAGE_ID, pageId)).iterator().toList();
    }

    /**
     * Check if an original AnnoPage exists that matches the given parameters using DBCollection.count().
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param pageId    index (page number) of the Annopage object
     * @return true if yes, otherwise false
     */
    public boolean existsOriginalByPageId(String datasetId, String localId, String pageId) {
        return datastore.find(AnnoPage.class).filter(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(PAGE_ID, pageId)
        ).count() > 0;
    }

    /**
     * Check if an original AnnoPage exists that matches the given parameters using DBCollection.count().
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param pageId    index (page number) of the Annopage object
     * @param lang      in which language should the original AnnoPage be
     * @return true if yes, otherwise false
     */
    public boolean existsOriginalByPageIdLang(String datasetId, String localId, String pageId, String lang) {
        return existsByPageIdLang(datasetId, localId, pageId, lang, AnnoPage.class);
    }

    /**
     * Check if a TranslationAnnoPage exists that matches the given parameters using DBCollection.count().
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param pageId    index (page number) of the Annopage object
     * @param lang      in which language should the translation be
     * @return true if yes, otherwise false
     */
    public boolean existsTranslationByPageIdLang(String datasetId, String localId, String pageId, String lang) {
        return existsByPageIdLang(datasetId, localId, pageId, lang, TranslationAnnoPage.class);
    }

    private boolean existsByPageIdLang(String datasetId, String localId, String pageId, String lang, Class clazz) {
        return datastore.find(clazz).filter(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(PAGE_ID, pageId),
                eq(LANGUAGE, lang)
        ).count() > 0;
    }

    /**
     * Check if an AnnoPage exists that contains an Annotation that matches the given parameters
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param annoId    ID of the annotation
     * @return true if yes, otherwise false
     */
    // TODO april 2020: Method is unused, remove?
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
     * Find and return an original AnnoPage that matches the given parameters.
     * Only annotations that match the specified text granularity values are retrieved from the data store.
     * <p>
     * The mongodb query implemented by this method is:
     * db.getCollection("AnnoPage").aggregate(
     * {$match: {"dsId": <datasetId>, "lcId": <localId>, "pgId": <pageId>}},
     * {$project: {
     *   "dsId": "$dsId",
     *   "lcId":"$lcId",
     *   "pgId": "$pgId",
     *   "tgtId": "$tgtId",
     *   "res": "$res",
     *   "className": "$className",
     *   "modified": "$modified",
     *   "ans": {
     *                 $filter: {
     *                   input: "$ans",
     *                   as: "annotation",
     *                   cond: { $in: [ '$$annotation.dcType', [<textGranValues>] ] }
     *                 }
     *             }
     * })
     *
     * @param datasetId      ID of the dataset
     * @param localId        ID of the parent of the Annopage object
     * @param pageId         index (page number) of the Annopage object
     * @param annoTypes      dcType values to filter annotations with
     * @return AnnoPage
     */
    public AnnoPage findOriginalByPageId(String datasetId, String localId, String pageId, List<AnnotationType> annoTypes) {
        Aggregation<AnnoPage> query = datastore.aggregate(AnnoPage.class).match(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(PAGE_ID, pageId)
        );
        query = filterTextGranularity(query, annoTypes);
        return query.execute(AnnoPage.class).tryNext();
    }

    /**
     * Find and return an original AnnoPage that matches the given parameters.
     * @param datasetId  ID of the dataset
     * @param localId    ID of the parent of the Annopage object
     * @param pageId     index (page number) of the Annopage object
     * @param lang       language
     * @param annoTypes  dcType values to filter annotations with
     * @return AnnoPage
     */
    public AnnoPage findOriginalByPageIdLang(String datasetId, String localId, String pageId, List<AnnotationType> annoTypes,
                                             String lang) {
        Aggregation<AnnoPage> query = datastore.aggregate(AnnoPage.class).match(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(PAGE_ID, pageId),
                eq(LANGUAGE, lang)
        );
        query = filterTextGranularity(query, annoTypes);
        return query.execute(AnnoPage.class).tryNext();
    }

    /**
     * Find and return a Translation AnnoPage that matches the given parameters.
     * @param datasetId  ID of the dataset
     * @param localId    ID of the parent of the Annopage object
     * @param pageId     index (page number) of the Annopage object
     * @param lang       language
     * @param annoTypes  dcType values to filter annotations with
     * @return AnnoPage
     */
    public TranslationAnnoPage findTranslationByPageIdLang(String datasetId, String localId, String pageId,
                                                           List<AnnotationType> annoTypes, String lang) {
        Aggregation<TranslationAnnoPage> query = datastore.aggregate(TranslationAnnoPage.class).match(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(PAGE_ID, pageId),
                eq(LANGUAGE, lang)
        );
        query = filterTextGranularity(query, annoTypes);
        return query.execute(TranslationAnnoPage.class).tryNext();
    }

    /**
     * Find and return original AnnoPage that contains an annotation that matches the given parameters
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param annoId    ID of the annotation
     * @return AnnoPage
     */
    public AnnoPage findOriginalByAnnoId(String datasetId, String localId, String annoId) {
        return findAnnotationById(datasetId, localId, annoId, AnnoPage.class);
    }

    /**
     * Find and return a Translation AnnoPage that contains an annotation that matches the given parameters
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param annoId    ID of the annotation
     * @return TranslationAnnoPage
     */
    public TranslationAnnoPage findTranslationByAnnoId(String datasetId, String localId, String annoId) {
        return (TranslationAnnoPage) findAnnotationById(datasetId, localId, annoId, TranslationAnnoPage.class);
    }

    private AnnoPage findAnnotationById(String datasetId, String localId, String annoId, Class clazz) {
        return (AnnoPage) datastore.find(clazz).filter(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(ANNOTATIONS_ID, annoId))
                .first();
    }

    /**
     * Find and return original AnnoPages that contains an annotation that matches the given parameters.
     *
     * Returns a {@link MorphiaCursor} that can be iterated on to obtain matching AnnoPages.
     * The cursor must be closed after iteration is completed.
     *
     * The Cursor returned by this method must be closed
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param imageIds  ID of the image
     * @param annoTypes type of annotations that should be retrieve, if null or empty all annotations of that
     *                        annopage will be retrieved
     * @return MorphiaCursor containing AnnoPage entries.
     */
    public MorphiaCursor<AnnoPage> findByImageId(String datasetId, String localId, List<String> imageIds,
                                                 List<AnnotationType> annoTypes) {
        Aggregation<AnnoPage> query = datastore.aggregate(AnnoPage.class).match(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                in(IMAGE_ID, imageIds)
        );
        query = filterTextGranularity(query, annoTypes);
        return query.execute(AnnoPage.class);
    }



    /**
     * Creates an AnnoPage aggregation query to return only matching annotation types.
     * @param annoPageQuery aggregation query
     * @param annoTypes list containing text granularity values to match
     * @return Updated aggregation query
     */
    private Aggregation filterTextGranularity(Aggregation annoPageQuery, List<AnnotationType> annoTypes) {
        if (annoTypes.isEmpty()) {
            return annoPageQuery;
        }

        // ans.dcType stored as first letter of text granularity value in uppercase. ie. WORD -> 'W'
        List<String> dcTypes = annoTypes.stream().map(s -> String.valueOf(s.getAbbreviation())).collect(Collectors.toUnmodifiableList());

        // _id implicitly included in projection
        return annoPageQuery.project(
                Projection.of()
                        .include(DATASET_ID)
                        .include(LOCAL_ID)
                        .include(PAGE_ID)
                        .include(RESOURCE)
                        .include(CLASSNAME)
                        .include(IMAGE_ID)
                        .include(MODIFIED)
                        .include(ANNOTATIONS,
                                filter(field(ANNOTATIONS),
                                        ArrayExpressions.in(value("$$annotation.dcType"), value(dcTypes))
                                ).as("annotation")
                        )
        );
    }

    /**
     * Creates a query which merges the AnnoPage with TranslationAnnoPage
     * based on datasetId and localId
     *
     * Query :
     *  collection.aggregate(Arrays.asList(new Document("$match",new Document("dsId", dsId).append("lcId", lcId)),
     *          new Document("$lookup",new Document("from", "TranslationAnnoPage")
     *           .append("let",new Document("origDsId", "$dsId")
     *                        .append("origLcId", "$lcId")
     *                        .append("origPgId", "$pgId"))
     *           .append("pipeline", Arrays.asList(new Document("$match",
     *                               new Document("$expr",
     *                               new Document("$and", Arrays.asList(new Document("$eq", Arrays.asList("$dsId", "$$origDsId")),
     *                                                                 new Document("$eq", Arrays.asList("$lcId", "$$origLcId")),
     *                                                                 new Document("$eq", Arrays.asList("$pgId", "$$origPgId")))))),
     *                               new Document("$project",new Document("dsId", 1L)
     *                                              .append("_id", 1L)
     *                                              .append("ldId", 1L)
     *                                              .append("pgId", 1L)
     *                                              .append("lang", 1L)
     *                                               .append("modified", 1L)
     *                                         )))
     *                               .append("as", "translations"))));
     * @param dsId
     * @param lcId
     */
    public void testLookUpMerge(String dsId, String lcId) {

        long start = System.currentTimeMillis();
        MongoDatabase database = datastore.getDatabase();
        MongoCollection<Document> collection = database.getCollection("AnnoPage");

        Map<String, Boolean> projectionFields = new HashMap<>();
        projectionFields.put(DATASET_ID, true);
        projectionFields.put(LOCAL_ID, true);
        projectionFields.put(PAGE_ID, true);
        projectionFields.put(LANGUAGE, true);
        projectionFields.put(MODIFIED, true);

        MongoCursor<Document> cursor =  collection.aggregate(Arrays.asList(createMatchFilter(dsId, lcId), getLookupPipeline(projectionFields))).iterator();

        LOG.info("Time taken to retrieve the documents {} ms" ,(System.currentTimeMillis() - start));
        if(cursor != null) {
            while(cursor.hasNext()) {
                Document object = cursor.next();
                System.out.println( "Annopage : { dsid : " + object.get(DATASET_ID) + ",  lcid : " +object.get(LOCAL_ID) + ",  pageId : " +object.get(PAGE_ID) + ", modified :"+ object.get(MODIFIED) + "}");
                System.out.println( "Translation : " + object.get(TRANSLATIONS).toString());
            }
        }
        LOG.info("Total Time taken by the method {} ms ", (System.currentTimeMillis() - start));
    }

    /**
     * Creates a $match filter for datasetId and localId
     * @param dataSetId
     * @param localId
     * @return
     */
    private Document createMatchFilter(String dataSetId, String localId){
        return new Document(MONGO_MATCH, new Document(DATASET_ID, dataSetId)
                        .append(LOCAL_ID, localId));
    }


    /**
     * Creates  the $lookup filter for the aggregation pipeline
     * from 'TranslationAnnoPage' collection
     * @param projectionFields
     * @return
     */
    private Document getLookupPipeline(Map<String, Boolean> projectionFields) {
        return new Document(MONGO_LOOKUP,
                new Document(MONGO_FROM, "TranslationAnnoPage")
                        .append(MONGO_LET,
                                new Document("origDsId", MONGO_DATASET_ID)
                                        .append("origLcId", MONGO_LOCAL_ID)
                                        .append("origPgId", MONGO_PAGE_ID))
                        .append(MONGO_PIPELINE, getPipeLineForFromCollection(projectionFields))
                        .append(MONGO_AS, TRANSLATIONS));
    }


    private List<Document> getPipeLineForFromCollection(Map<String, Boolean> projectionFields) {
        Document matchExprePipeline = new Document(MONGO_MATCH,
                new Document(MONGO_EXPRESSION,
                        new Document(MONGO_AND, Arrays.asList(new Document(MONGO_EQUALS, Arrays.asList(MONGO_DATASET_ID, "$$origDsId")),
                                new Document(MONGO_EQUALS, Arrays.asList(MONGO_LOCAL_ID, "$$origLcId")),
                                new Document(MONGO_EQUALS, Arrays.asList(MONGO_PAGE_ID, "$$origPgId"))))));

        Document projection = getProjectionFields(projectionFields);
        if (projection != null) {
            return Arrays.asList(matchExprePipeline, getProjectionFields(projectionFields));
        } else {
            return Arrays.asList(matchExprePipeline);
        }
    }

    /**
     * Returns the Projection fields with inclusion and exclusion values
     * @param projectionFields
     * @return
     */
    private Document getProjectionFields(Map<String, Boolean> projectionFields) {
        if (!projectionFields.isEmpty()) {
            Document d = new Document();
            for (Map.Entry<String, Boolean> entry : projectionFields.entrySet()) {
                long inclusion = entry.getValue() ? 1L : 0L;
                d.append(entry.getKey(), inclusion);
            }
            return new Document(MONGO_PROJECT, d);
        }
        return null;
    }
}
