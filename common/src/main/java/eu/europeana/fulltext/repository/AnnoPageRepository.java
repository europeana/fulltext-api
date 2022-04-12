package eu.europeana.fulltext.repository;

import com.mongodb.DBRef;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.expressions.ArrayExpressions;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.query.FindOptions;
import dev.morphia.query.experimental.filters.Filter;
import dev.morphia.query.internal.MorphiaCursor;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltext.entity.TranslationResource;
import eu.europeana.fulltext.exception.DatabaseQueryException;
import eu.europeana.fulltext.util.MorphiaUtils;
import java.time.Instant;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import java.util.Arrays;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.util.*;
import java.util.stream.Collectors;

import static dev.morphia.aggregation.experimental.expressions.ArrayExpressions.filter;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.expressions.Expressions.value;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.in;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.*;
import static eu.europeana.fulltext.util.MorphiaUtils.SET;
import static eu.europeana.fulltext.util.MorphiaUtils.SET_ON_INSERT;
import static eu.europeana.fulltext.util.MorphiaUtils.TRANSLATION_RESOURCE_COL;
import static eu.europeana.fulltext.util.MorphiaUtils.UPSERT_OPTS;


/**
 * Repository for retrieving AnnoPage objects / data Created by luthien on 31/05/2018.
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

    private long count(Class<? extends AnnoPage> clazz) {
        return datastore.getMapper().getCollection(clazz).countDocuments();
    }

    /**
     * Check if any AnnoPages exist that match the given parameters using DBCollection.count().
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @return true if yes, otherwise false
     */
    public long existForEuropeanaId(String datasetId, String localId, Class claph) {
        return datastore.find(claph).filter(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId))
            .count();
    }

    /**
     * Find and return AnnoPages that match the given parameters.
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @return List of AnnoPage objects
     */
    public List<AnnoPage> findOrigPages(String datasetId, String localId) {
        //TODO instead of loading the AnnoPage + Resource, we should load have the option to only the AnnoPage
        return datastore.find(AnnoPage.class)
            .filter(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId))
            .iterator()
            .toList();
    }

    /**
     * Find and return single AnnoPage that match the given parameters.
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @return AnnoPage
     */
    public AnnoPage findPage(String datasetId, String localId) {
        return datastore.find(AnnoPage.class)
            .filter(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId)).first();
    }

    /**
     * Find and return TranslationAnnoPages that match the given parameters using DBCollection.count(). The Morphia
     * ReferenceException is thrown if there is no matching TranslationResource document found;
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the TranslationAnnopage object
     * @param pageId    index (page number) of the TranslationAnnopage object
     * @return List of TranslationAnnopage objects
     */
    public List<TranslationAnnoPage> findTranslatedPages(String datasetId, String localId,
        String pageId) {
        // TODO instead of loading the AnnoPage + Resource, we should load only the AnnoPage
        return datastore.find(TranslationAnnoPage.class).filter(eq(DATASET_ID, datasetId),
            eq(LOCAL_ID, localId),
            eq(PAGE_ID, pageId)).iterator().toList();
    }

    /**
     * Check if an original AnnoPage exists that matches the given parameters using DBCollection.count().
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param pageId    index (page number) of the Annopage object
     * @return true if yes, otherwise false
     */
    public boolean existsOriginalByPageId(String datasetId, String localId, String pageId) {
        return datastore.find(AnnoPage.class).filter(eq(DATASET_ID, datasetId),
            eq(LOCAL_ID, localId),
            eq(PAGE_ID, pageId)).count() > 0;
    }

    /**
     * Check if an original AnnoPage exists that matches the given parameters using DBCollection.count().
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param pageId    index (page number) of the Annopage object
     * @param lang      in which language should the original AnnoPage be
     * @return true if yes, otherwise false
     */
    public boolean existsOriginalByPageIdLang(String datasetId, String localId, String pageId,
        String lang) {
        return existsByPageIdLang(datasetId, localId, pageId, lang, AnnoPage.class);
    }

    /**
     * Check if a TranslationAnnoPage exists that matches the given parameters using DBCollection.count().
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param pageId    index (page number) of the Annopage object
     * @param lang      in which language should the translation be
     * @return true if yes, otherwise false
     */
    public boolean existsTranslationByPageIdLang(String datasetId, String localId, String pageId,
        String lang) {
        return existsByPageIdLang(datasetId, localId, pageId, lang, TranslationAnnoPage.class);
    }

    private boolean existsByPageIdLang(String datasetId, String localId, String pageId, String lang,
        Class<? extends AnnoPage> clazz) {
        List<Filter> filter =
            new ArrayList<>(
                Arrays.asList(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId), eq(PAGE_ID, pageId)));

        if (StringUtils.isNotEmpty(lang)) {
            filter.add(eq(LANGUAGE, lang));
        }
        return datastore.find(clazz).filter(filter.toArray(new Filter[0])).count()
            > 0L;
    }

    /**
     * Check if an AnnoPage exists that contains an Annotation that matches the given parameters
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param annoId    ID of the annotation
     * @return true if yes, otherwise false
     */
    // TODO april 2020: Method is unused, remove?
    public boolean existsWithAnnoId(String datasetId, String localId, String annoId) {
        return datastore.find(AnnoPage.class).filter(eq(DATASET_ID, datasetId),
            eq(LOCAL_ID, localId),
            eq(ANNOTATIONS_ID, annoId)).count() > 0;
    }

    /**
     * Find and return an original AnnoPage that matches the given parameters. Only annotations that match the specified
     * text granularity values are retrieved from the data store.
     * <p>
     * The mongodb query implemented by this method is: db.getCollection("AnnoPage").aggregate( {$match: {"dsId":
     * <datasetId>, "lcId": <localId>, "pgId": <pageId>}}, {$project: { "dsId": "$dsId", "lcId":"$lcId", "pgId":
     * "$pgId", "tgtId": "$tgtId", "res": "$res", "className": "$className", "modified": "$modified", "ans": { $filter:
     * { input: "$ans", as: "annotation", cond: { $in: [ '$$annotation.dcType', [<textGranValues>] ] } } } })
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param pageId    index (page number) of the Annopage object
     * @param annoTypes dcType values to filter annotations with
     * @return AnnoPage
     */
    public AnnoPage findOriginalByPageId(
        String datasetId, String localId, String pageId, List<AnnotationType> annoTypes) {
        Aggregation<AnnoPage> query = datastore.aggregate(AnnoPage.class)
            .match(eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(PAGE_ID, pageId));
        query = filterTextGranularity(query, annoTypes);
        return query.execute(AnnoPage.class).tryNext();
    }

    /**
     * Find and return an original AnnoPage that matches the given parameters.
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param pageId    index (page number) of the Annopage object
     * @param lang      language
     * @param annoTypes dcType values to filter annotations with
     * @return AnnoPage
     */
    public AnnoPage findOriginalByPageIdLang(
        String datasetId, String localId, String pageId, List<AnnotationType> annoTypes,
        String lang) {
        Aggregation<AnnoPage> query = datastore.aggregate(AnnoPage.class)
            .match(eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(PAGE_ID, pageId),
                eq(LANGUAGE, lang));
        query = filterTextGranularity(query, annoTypes);
        return query.execute(AnnoPage.class).tryNext();
    }

    /**
     * Find and return a Translation AnnoPage that matches the given parameters.
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param pageId    index (page number) of the Annopage object
     * @param lang      language
     * @param annoTypes dcType values to filter annotations with
     * @return AnnoPage
     */
    public TranslationAnnoPage findTranslationByPageIdLang(
        String datasetId, String localId, String pageId, List<AnnotationType> annoTypes,
        String lang) {
        List<Document> aggregatePipeLine = MorphiaUtils.getAggregatePipelineForTranslation(datasetId, localId, pageId, lang);
        // add the filter projection based on Text Granularity values
        if (!annoTypes.isEmpty()) {
            aggregatePipeLine.add(filterOnTextGranularity(annoTypes));
        }
        // as for translation lang parameter is passed, only one AnnoPage will be returned
        Document result = datastore
                .getDatabase()
                .getCollection(TranslationAnnoPage.class.getSimpleName())
                .aggregate(aggregatePipeLine)
                .iterator()
                .tryNext();
        return MorphiaUtils.processMongoDocument(result, datasetId, localId, pageId, lang);
        }

    /**
     * Gets the filter projection based on DcTypes values for annotations
     * @param annoType
     * @return
     */
    private Document filterOnTextGranularity(List<AnnotationType> annoType) {
        return new Document(DATASET_ID, 1L)
                .append(LOCAL_ID, 1L)
                .append(PAGE_ID, 1L)
                .append(RESOURCE, 1L)
                .append(TARGET_ID, 1L)
                .append(MODIFIED, 1L)
                .append(ANNOTATIONS,
                        new Document(MONGO_FILTER,
                                new Document(MONGO_INPUT, MONGO_ANNOTATIONS)
                                        .append(MONGO_AS, ANNOTATIONS)
                                        .append(MONGO_CONDITION,
                                                new Document(MONGO_IN, Arrays.asList(MONGO_FILTER_ANS_DCTYPE, getDcTypes(annoType))))));
    }

    /**
     * Find and return original AnnoPage that contains an annotation that matches the given parameters
     *
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
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param annoId    ID of the annotation
     * @return TranslationAnnoPage
     */
    public TranslationAnnoPage findTranslationByAnnoId(String datasetId, String localId,
        String annoId) {
        return (TranslationAnnoPage) findAnnotationById(datasetId, localId, annoId,
            TranslationAnnoPage.class);
    }

    private AnnoPage findAnnotationById(String datasetId, String localId, String annoId,
        Class clazz) {
        return (AnnoPage) datastore.find(clazz).filter(eq(DATASET_ID, datasetId),
            eq(LOCAL_ID, localId),
            eq(ANNOTATIONS_ID, annoId)).first();
    }

    /**
     * Find and return original AnnoPages that contains an annotation that matches the given parameters.
     * <p>
     * Returns a {@link MorphiaCursor} that can be iterated on to obtain matching AnnoPages. The cursor must be closed
     * after iteration is completed.
     * <p>
     * The cursor returned by this method must be closed
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param targetIds IDs of the target(s) / image(s)
     * @param annoTypes type of annotations that should be retrieved, if null or empty all annotations of that annopage
     *                  will be retrieved
     * @return MorphiaCursor containing AnnoPage entries.
     */
    public MorphiaCursor<AnnoPage> findByTargetId(
        String datasetId, String localId, List<String> targetIds, List<AnnotationType> annoTypes) {
        Aggregation<AnnoPage> query = datastore.aggregate(AnnoPage.class)
            .match(eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                in(TARGET_ID, targetIds));
        query = filterTextGranularity(query, annoTypes);
        return query.execute(AnnoPage.class);
    }


    public UpdateResult updateAnnoPage(TranslationAnnoPage annoPage) {
        MongoCollection<TranslationAnnoPage> collection =
            datastore.getMapper().getCollection(TranslationAnnoPage.class);
        return collection.updateOne(
            new Document(
                Map.of(
                    DATASET_ID,
                    annoPage.getDsId(),
                    LOCAL_ID,
                    annoPage.getLcId(),
                    PAGE_ID,
                    annoPage.getPgId(),
                    LANGUAGE,
                    annoPage.getLang())),
            new Document(
                "$set",
                new Document(ANNOTATIONS, annoPage.getAns())
                    .append(MODIFIED, annoPage.getModified())
                    .append(SOURCE, annoPage.getSource())));
    }

    /**
     * Saves an TranslationAnnoPage to the database
     *
     * @param annoPage TranslationAnnoPage object to save
     * @return the saved TranslationAnnoPage document
     */
    public TranslationAnnoPage saveAnnoPage(TranslationAnnoPage annoPage) {
        return datastore.save(annoPage);
    }

    public BulkWriteResult upsert(List<? extends TranslationAnnoPage> annoPageList)
        throws DatabaseQueryException {
        MongoCollection<TranslationAnnoPage> annoPageCollection =
            datastore.getMapper().getCollection(TranslationAnnoPage.class);

        List<WriteModel<TranslationAnnoPage>> annoPageUpdates = new ArrayList<>();

        Instant now = Instant.now();

        for (TranslationAnnoPage annoPage : annoPageList) {
            annoPageUpdates.add(createAnnoPageUpdate(now, annoPage));
        }

        return annoPageCollection.bulkWrite(annoPageUpdates);
    }

    /**
     * Creates an AnnoPage aggregation query to return only matching annotation types.
     *
     * @param annoPageQuery aggregation query
     * @param annoTypes     list containing text granularity values to match
     * @return Updated aggregation query
     */
    private Aggregation filterTextGranularity(Aggregation annoPageQuery,
        List<AnnotationType> annoTypes) {
        if (annoTypes.isEmpty()) {
            return annoPageQuery;
        }

        List<String> dcTypes = getDcTypes(annoTypes);
        // _id implicitly included in projection
        return annoPageQuery.project(Projection.of()
            .include(DATASET_ID)
            .include(LOCAL_ID)
            .include(PAGE_ID)
            .include(RESOURCE)
            .include(CLASSNAME)
            .include(TARGET_ID)
            .include(MODIFIED)
            .include(ANNOTATIONS,
                filter(field(ANNOTATIONS),
                    ArrayExpressions.in(value("$$annotation.dcType"),
                        value(dcTypes))).as("annotation")));
    }

    /**
     * Gets the List of annoTypes in letters
     * ans.dcType stored as first letter of text granularity value in uppercase. ie. WORD -> 'W'
     * @param annoTypes
     * @return
     */
    private List<String> getDcTypes(List<AnnotationType> annoTypes) {
        return annoTypes.stream()
                .map(s -> String.valueOf(s.getAbbreviation()))
                .collect(Collectors.toUnmodifiableList());
    }


    public List<Document> getAnnoPageAndTranslations(String dsId, String lcId) {
        List<Document> annoPagesWithTranslations = new ArrayList<>();

        MongoDatabase database = datastore.getDatabase();
        MongoCollection<Document> collection = database.getCollection("AnnoPage");

        Map<String, Boolean> projectionTapFields = new HashMap<>();
        projectionTapFields.put(DOC_ID, false);
        projectionTapFields.put(LANGUAGE, true);
        projectionTapFields.put(MODIFIED, true);
        Map<String, Boolean> projectionApFields = new HashMap<>();
        projectionTapFields.put(DOC_ID, false);
        projectionApFields.put(DATASET_ID, true);
        projectionApFields.put(LOCAL_ID, true);
        projectionApFields.put(PAGE_ID, true);
        projectionApFields.put(LANGUAGE, true);
        projectionApFields.put(MODIFIED, true);
        projectionApFields.put(TRANSLATIONS, true);

        for (Document apWt : collection.aggregate(Arrays.asList(
                                                    createMatchFilter(dsId, lcId),
                                                    getLookupPipeline(projectionTapFields),
                                                    getProjectionFields(projectionApFields)
                                                 ))){
            annoPagesWithTranslations.add(apWt);
        }
        return annoPagesWithTranslations;
    }

    public boolean annoPageExistsByTgtId(
        String datasetId, String localId, String targetId, String lang) {
        return datastore
            .find(TranslationAnnoPage.class)
            .filter(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(LANGUAGE, lang),
                eq(TARGET_ID, targetId))
            .count()
            > 0L;
    }

    private Document createMatchFilter(String dataSetId, String localId) {
        return new Document(MONGO_MATCH,
            new Document(DATASET_ID, dataSetId).append(LOCAL_ID, localId));
    }

    private Document getLookupPipeline(Map<String, Boolean> projectionFields) {
        return new Document(MONGO_LOOKUP,
            new Document(MONGO_FROM, "TranslationAnnoPage")
                .append(MONGO_LET, new Document("origDsId", MONGO_DATASET_ID)
                    .append("origLcId", MONGO_LOCAL_ID)
                    .append("origPgId", MONGO_PAGE_ID))
                .append(MONGO_PIPELINE, getPipeLineForFromCollection(projectionFields))
                .append(MONGO_AS, TRANSLATIONS));
    }

    private List<Document> getPipeLineForFromCollection(Map<String, Boolean> projectionFields) {
        Document matchExprePipeline = new Document(MONGO_MATCH,
            new Document(MONGO_EXPRESSION,
                new Document(MONGO_AND,
                    Arrays.asList(
                        new Document(MONGO_EQUALS, Arrays.asList(MONGO_DATASET_ID, "$$origDsId")),
                        new Document(MONGO_EQUALS, Arrays.asList(MONGO_LOCAL_ID, "$$origLcId")),
                        new Document(MONGO_EQUALS, Arrays.asList(MONGO_PAGE_ID, "$$origPgId"))))));

        Document projection = getProjectionFields(projectionFields);
        if (projection != null) {
            return Arrays.asList(matchExprePipeline, projection);
        } else {
            return Arrays.asList(matchExprePipeline);
        }
    }

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

    private UpdateOneModel<TranslationAnnoPage> createAnnoPageUpdate(
        Instant now, TranslationAnnoPage annoPage) throws DatabaseQueryException {

        TranslationResource res = annoPage.getRes();

        if (res == null) {
            // all AnnoPages should have a resource
            throw new DatabaseQueryException("res is null for " + annoPage);
        }

        Document updateDoc =
            new Document(DATASET_ID, annoPage.getDsId())
                .append(LOCAL_ID, annoPage.getLcId())
                .append(PAGE_ID, annoPage.getPgId())
                .append(TARGET_ID, annoPage.getTgtId())
                .append(ANNOTATIONS, annoPage.getAns())
                .append(MODIFIED, now)
                .append(LANGUAGE, annoPage.getLang());

        // source isn't always set. Prevent null from being saved in db
        if (annoPage.getSource() != null) {
            updateDoc.append(SOURCE, annoPage.getSource());
        }

        return new UpdateOneModel<>(
            new Document(
                // filter
                Map.of(
                    DATASET_ID,
                    annoPage.getDsId(),
                    LOCAL_ID,
                    annoPage.getLcId(),
                    LANGUAGE,
                    annoPage.getLang(),
                    PAGE_ID,
                    annoPage.getPgId())),
            new Document(SET, updateDoc)
                // Only link resource for new documents. Resource ref should not change otherwise
                .append(
                    SET_ON_INSERT,
                    new Document(RESOURCE, new DBRef(TRANSLATION_RESOURCE_COL, res.getId()))),
            UPSERT_OPTS);
    }

    public long deleteAnnoPage(String datasetId, String localId, String pageId, String lang) {
        return datastore
            .find(TranslationAnnoPage.class)
            .filter(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(PAGE_ID, pageId),
                eq(LANGUAGE, lang))
            .delete()
            .getDeletedCount();
    }

    public long deleteAnnoPages(String datasetId, String localId, String pageId) {
        return datastore
            .find(TranslationAnnoPage.class)
            .filter(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId), eq(PAGE_ID, pageId))
            .delete(MorphiaUtils.MULTI_DELETE_OPTS)
            .getDeletedCount();
    }

    public TranslationAnnoPage getAnnoPageWithSource(String source, boolean fetchFullDoc) {
        FindOptions findOptions = new FindOptions().limit(1);

        if (!fetchFullDoc) {
            findOptions =
                findOptions
                    .projection()
                    .include(DATASET_ID, LOCAL_ID, PAGE_ID, TARGET_ID, LANGUAGE, SOURCE);
        }

        return datastore
            .find(TranslationAnnoPage.class)
            .filter(eq(SOURCE, source))
            .iterator(findOptions)
            .tryNext();
    }

    public long deleteAnnoPagesWithSources(List<? extends String> sources) {
        return datastore
            .find(TranslationAnnoPage.class)
            .filter(in(SOURCE, sources))
            .delete(MorphiaUtils.MULTI_DELETE_OPTS)
            .getDeletedCount();
    }

    /** Only for tests */
    public void deleteAll() {
        datastore.find(TranslationAnnoPage.class).delete(MorphiaUtils.MULTI_DELETE_OPTS);
    }
}
