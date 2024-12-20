package eu.europeana.fulltext.repository;

import static dev.morphia.aggregation.experimental.expressions.ArrayExpressions.filter;
import static dev.morphia.aggregation.experimental.expressions.ArrayExpressions.reduce;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.expressions.Expressions.value;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.in;
import static dev.morphia.query.experimental.updates.UpdateOperators.set;
import static dev.morphia.query.experimental.updates.UpdateOperators.unset;
import static eu.europeana.fulltext.util.MorphiaUtils.*;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.*;

import com.mongodb.DBRef;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.expressions.ArrayExpressions;
import dev.morphia.aggregation.experimental.expressions.Expressions;
import dev.morphia.aggregation.experimental.expressions.SetExpressions;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Sort;
import dev.morphia.query.experimental.filters.Filter;
import dev.morphia.query.internal.MorphiaCursor;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.exception.DatabaseQueryException;
import eu.europeana.fulltext.util.MorphiaUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;


/**
 * Repository for retrieving AnnoPage objects / data Created by luthien on 31/05/2018.
 */
@Repository
public class AnnoPageRepository {

    @Value("${spring.profiles.active:}")
    private String activeProfileString;

    @Autowired
    protected Datastore datastore;

    public long count() {
        return datastore.getMapper().getCollection(AnnoPage.class).countDocuments();
    }

    /**
     * Check if any AnnoPages exist that match the given parameters using DBCollection.count().
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @return true if yes, otherwise false
     */
    public long existForEuropeanaId(String datasetId, String localId) {
        return datastore.find(AnnoPage.class).filter(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId))
            .count();
    }

  /**
   * Find and return AnnoPages that match the given parameters.
   * Also includes deprecated AnnoPages.
   * @param datasetId ID of the dataset
   * @param localId ID of the parent of the Annopage object
   * @param projectionFields fields to populate in the result
   * @return List of AnnoPage objects
   */
  public List<AnnoPage> findAnnoPages(
      String datasetId, String localId, List<String> projectionFields) {

    FindOptions findOptions = new FindOptions();
    if (projectionFields != null && !projectionFields.isEmpty()) {
      findOptions.projection().include(projectionFields.toArray(String[]::new));
    }

    return datastore
        .find(AnnoPage.class)
        .filter(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId))
        .iterator(findOptions)
        .toList();
  }

    /**
     * Find and return single AnnoPage that match the given parameters.
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @return AnnoPage
     */
    public AnnoPage findPage(String datasetId, String localId, boolean fectchFullDoc) {
        FindOptions findOptions = new FindOptions().limit(1);
        /**
         * EA-3994 for performance we will fetch only few fields
         * and Fetch the most recent AnooPage for generating etag for caching
         */
        if (!fectchFullDoc) {
            findOptions
                    .projection()
                    .include(DATASET_ID, LOCAL_ID, PAGE_ID, MODIFIED);
        }
        findOptions.sort(Sort.descending(MODIFIED));

        return datastore.find(AnnoPage.class)
                .filter(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId))
                .iterator(findOptions).tryNext();
    }

    /**
     * Check if an  AnnoPage exists that matches the given parameters using DBCollection.count().
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param pageId    index (page number) of the Annopage object
     * @param includeDeprecated
     * @return true if yes, otherwise false
     */
    public boolean existsByPageId(String datasetId, String localId, String pageId,
        boolean includeDeprecated) {
    List<Filter> filters =
        new ArrayList<>(
            Arrays.asList(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId), eq(PAGE_ID, pageId)));

    if (!includeDeprecated) {
      filters.add(eq(DELETED, null));
    }

    return datastore.find(AnnoPage.class).filter(filters.toArray(new Filter[0])).count() > 0;
    }

  public boolean existsOriginalByPageId(
      String datasetId, String localId, String pageId, boolean includeDeprecated) {
    List<Filter> filters =
        new ArrayList<>(
            Arrays.asList(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(PAGE_ID, pageId),
                eq(TRANSLATION, null)));

    if (!includeDeprecated) {
      filters.add(eq(DELETED, null));
    }

    return datastore.find(AnnoPage.class).filter(filters.toArray(new Filter[0])).count() > 0;
  }

    /**
     * Check if an  AnnoPage exists that matches the given parameters using DBCollection.count().
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param pageId    index (page number) of the Annopage object
     * @param lang      in which language should the  AnnoPage be
     * @param includeDeprecated indicates whether deprecated AnnoPages should be included in result
     * @return true if yes, otherwise false
     */
    public boolean existsByPageIdLang(String datasetId, String localId, String pageId,
        String lang, boolean includeDeprecated) {
        List<Filter> filter =
            new ArrayList<>(
                Arrays.asList(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId), eq(PAGE_ID, pageId)));

        if (StringUtils.isNotEmpty(lang)) {
            filter.add(eq(LANGUAGE, lang));
        }

        if(!includeDeprecated){
            filter.add(eq(DELETED, null));
        }
        return datastore.find(AnnoPage.class).filter(filter.toArray(new Filter[0])).count()
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
    public AnnoPage findByPageId(
        String datasetId, String localId, String pageId, List<AnnotationType> annoTypes, boolean includeDeprecated) {

        List<Filter> filters =
            new ArrayList<>(
                Arrays.asList(eq(DATASET_ID, datasetId),
                    eq(LOCAL_ID, localId),
                    eq(PAGE_ID, pageId)));

        if(!includeDeprecated){
            filters.add(eq(DELETED, null));
        }

        Aggregation<AnnoPage> query = datastore.aggregate(AnnoPage.class)
            .match(filters.toArray(new Filter[0]));
        query = filterTextGranularity(query, annoTypes);
        return query.execute(AnnoPage.class).tryNext();
    }

    /**
     * Find and return an AnnoPage that matches the given parameters.
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param pageId    index (page number) of the Annopage object
     * @param annoTypes dcType values to filter annotations with
     * @param lang      language
     * @param includeDeprecated whether deprecated AnnoPages should be included in result
     * @return AnnoPage
     */
    public AnnoPage findByPageIdLang(
        String datasetId, String localId, String pageId, List<AnnotationType> annoTypes,
        String lang, boolean includeDeprecated) {

        List<Filter> filters =
            new ArrayList<>(
                Arrays.asList(eq(DATASET_ID, datasetId),
                    eq(LOCAL_ID, localId),
                    eq(PAGE_ID, pageId),
                    eq(LANGUAGE, lang)));

        if(!includeDeprecated){
            filters.add(eq(DELETED, null));
        }

        Aggregation<AnnoPage> query = datastore.aggregate(AnnoPage.class)
            .match(filters.toArray(new Filter[0]));
        query = filterTextGranularity(query, annoTypes);
        return query.execute(AnnoPage.class).tryNext();
    }

    /**
     * Finds the original AnnoPage with the given parameters.
     * Original means the "translation" field in the database is empty
     * @param datasetId
     * @param localId
     * @param pageId
     * @param annoTypes
     * @param includeDeprecated
     * @return
     */
    public AnnoPage findOriginalByPageId(String datasetId, String localId, String pageId,
        List<AnnotationType> annoTypes, boolean includeDeprecated) {
        List<Filter> filters =
            new ArrayList<>(
                Arrays.asList(eq(DATASET_ID, datasetId),
                    eq(LOCAL_ID, localId),
                    eq(PAGE_ID, pageId),
                    eq(TRANSLATION, null)));

        if(!includeDeprecated){
            filters.add(eq(DELETED, null));
        }

        Aggregation<AnnoPage> query = datastore.aggregate(AnnoPage.class)
            .match(filters.toArray(new Filter[0]));
        query = filterTextGranularity(query, annoTypes);
        return query.execute(AnnoPage.class).tryNext();
    }


    /**
     * Find and return AnnoPage that contains an annotation that matches the given parameters
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param annoId    ID of the annotation
     * @return AnnoPage
     */
    public AnnoPage findByAnnoId(String datasetId, String localId, String annoId, boolean includeDeprecated) {

        List<Filter> filter =
            new ArrayList<>(
                Arrays.asList(eq(DATASET_ID, datasetId),
                    eq(LOCAL_ID, localId),
                    eq(ANNOTATIONS_ID, annoId)));

        if(!includeDeprecated){
            filter.add(eq(DELETED, null));
        }

        return datastore.find(AnnoPage.class).filter(filter.toArray(new Filter[0])).first();
    }

    /**
     * Find and return AnnoPages that contains an annotation that matches the given parameters.
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
        String datasetId, String localId, List<String> targetIds, List<AnnotationType> annoTypes, boolean includeDeprecated) {

        List<Filter> filter =
            new ArrayList<>(
                Arrays.asList(eq(DATASET_ID, datasetId),
                    eq(LOCAL_ID, localId),
                    in(TARGET_ID, targetIds)));

        if(!includeDeprecated){
            filter.add(eq(DELETED, null));
        }

        Aggregation<AnnoPage> query = datastore.aggregate(AnnoPage.class)
            .match(filter.toArray(new Filter[0]));
        query = filterTextGranularity(query, annoTypes);
        return query.execute(AnnoPage.class);
    }


    /**
     * Updates the given AnnoPage.
     * This call bypasses Morphia, so the AnnoPage object doesn't need to have
     * an _id value set
     * @param annoPage AnnoPage to update
     * @return UpdateResult
     */
    public UpdateResult updateAnnoPage(AnnoPage existingAnnoPage, AnnoPage annoPage) {
        MongoCollection<AnnoPage> collection =
            datastore.getMapper().getCollection(AnnoPage.class);

        Document updateDoc = new Document(ANNOTATIONS, annoPage.getAns())
                .append(MODIFIED, annoPage.getModified())
                .append(SOURCE, annoPage.getSource());

        // if annopage is deprecated then update resource ref as well
        if (existingAnnoPage.isDeprecated()) {
            updateDoc.append(RESOURCE, new DBRef(RESOURCE_COL, annoPage.getRes().getId()));
        }

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
            new Document(SET, updateDoc)
                  .append(UNSET, new Document(DELETED, ""))); // while updating, annopage should not be deprecated
    }

    /**
     * Saves an AnnoPage to the database
     *
     * @param annoPage AnnoPage object to save
     * @return the saved AnnoPage document
     */
    public AnnoPage saveAnnoPage(AnnoPage annoPage) {
        return datastore.save(annoPage);
    }

    /**
     * "Upserts" the AnnoPages to the database
     * @param annoPageList list of AnnoPages to upsert
     * @return BulkWriteResult of operation
     * @throws DatabaseQueryException if list contains an invalid AnnoPage (ie. missing Resource)
     */
    public BulkWriteResult upsertAnnoPages(List<? extends AnnoPage> annoPageList)
        throws DatabaseQueryException {
        MongoCollection<AnnoPage> annoPageCollection =
            datastore.getMapper().getCollection(AnnoPage.class);

        List<WriteModel<AnnoPage>> annoPageUpdates = new ArrayList<>();

        Instant now = Instant.now();

        for (AnnoPage annoPage : annoPageList) {
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
    private Aggregation<AnnoPage> filterTextGranularity(Aggregation<AnnoPage> annoPageQuery,
        List<AnnotationType> annoTypes) {
        if (annoTypes.isEmpty()) {
            return annoPageQuery;
        }

        List<String> dcTypes = getDcTypes(annoTypes);
        // _id implicitly included in projection
        return annoPageQuery.project(Projection.project()
            .include(DATASET_ID)
            .include(LOCAL_ID)
            .include(PAGE_ID)
            .include(RESOURCE)
            .include(CLASSNAME)
            .include(TARGET_ID)
            .include(MODIFIED)
            .include(LANGUAGE) // EA-3123 lang, translation and deleted field is needed since multilingual behaviour and deprecation is added.
            .include(TRANSLATION)
            .include(DELETED)
            .include(SOURCE) // EA-3216, expose source value to differentiate between annotation coming from Annotation API and one submitted via Fullext API
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

    /**
     * Fetches List of AnnoPage.
     * @param datasetId
     * @param localId
     * @param pageId
     * @param includeDeprecated
     * @return
     */
    public List<AnnoPage> getAnnoPages(String datasetId, String localId, String pageId, boolean includeDeprecated, boolean fetchAnnotations) {
        FindOptions findOptions = new FindOptions()
                        .projection()
                        .include(DATASET_ID, LOCAL_ID, PAGE_ID, LANGUAGE, MODIFIED, DELETED, SOURCE); // EA-3216, include source field

        if (fetchAnnotations) {
            findOptions.projection().include(TRANSLATION, ANNOTATIONS); // EA-3457 add translation and annotations in the projections
        }

        return datastore
                .find(AnnoPage.class)
                .filter(createFilterToGetAnnoPage(datasetId, localId, pageId, null, includeDeprecated).toArray(new Filter[0]))
                .iterator(findOptions)
                .toList();
    }

    /**
     * fetches annopages with unique set of dcTypes that exists in all the annotations
     * EA- 3994 : This is done improve the performance of the query. We do not want to load
     *            all the annotations in memory to just process the unique dcType values.
     * @param datasetId
     * @param localId
     * @param pageId
     * @param includeDeprecated
     * @return
     */
    public List<AnnoPage> getAnnoPagesWithAnnotationsDcType(String datasetId, String localId, String pageId, boolean includeDeprecated) {
        Projection projection = Projection.project()
                .include(DATASET_ID)
                .include(LOCAL_ID)
                .include(PAGE_ID)
                .include(LANGUAGE)
                .include(MODIFIED)
                .include(DELETED)
                .include(SOURCE) // EA-3216, include source field
                .include(TRANSLATION); // EA-3457 add translation

        // add projection to fetch unique set of dctypes available in all the annotations
        projection.include(TEXT_GRANULARITY,
                    reduce(
                            Expressions.value(MONGO_ANNOTATIONS),
                            Expressions.value(Arrays.asList()),
                            SetExpressions.setUnion(
                                    Expressions.value("$$value"),
                                    Expressions.value(Arrays.asList("$$this.dcType")))));

        Aggregation<AnnoPage> query = datastore.aggregate(AnnoPage.class)
                .match(createFilterToGetAnnoPage(datasetId, localId, pageId, null, includeDeprecated).toArray(new Filter[0]))
                .project(projection);
        List<Document> ann1 = query.execute(Document.class).toList();


        // ugly solution to map TEXT_GRANULARITY
        List<AnnoPage> annopages = new ArrayList<>();
        for(Document annoPageDoc : ann1) {
            AnnoPage annoPage = new AnnoPage();
            annoPage.setDsId(annoPageDoc.getString(DATASET_ID));
            annoPage.setLcId(annoPageDoc.getString(LOCAL_ID));
            annoPage.setPgId(annoPageDoc.getString(PAGE_ID));
            annoPage.setLang(annoPageDoc.getString(LANGUAGE));
            annoPage.setModified(annoPageDoc.getDate(MODIFIED));
            // set optional fields
            if (annoPageDoc.containsKey(DELETED)) {
                annoPage.setDeleted(annoPageDoc.getDate(DELETED));
            }
            if (annoPageDoc.containsKey(SOURCE)) {
                annoPage.setSource(annoPageDoc.getString(SOURCE));
            }
            if (annoPageDoc.containsKey(TRANSLATION)) {
                annoPage.setTranslation(annoPageDoc.getBoolean(TRANSLATION));
            }
            annoPage.setTextGranularity(annoPageDoc.getList(TEXT_GRANULARITY, String.class));
            annopages.add(annoPage);
        }

      //  List<AnnoPage> annopages =  query.execute(AnnoPage.class).toList();
        return annopages;
    }


    private UpdateOneModel<AnnoPage> createAnnoPageUpdate(
        Instant now, AnnoPage annoPage) throws DatabaseQueryException {

        Resource res = annoPage.getRes();

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
                .append(LANGUAGE, annoPage.getLang())
                // link resources for new and deprecated documents
                .append(RESOURCE, new DBRef(RESOURCE_COL, res.getId()));

        // source isn't always set. Prevent null from being saved in db
        if (annoPage.getSource() != null) {
            updateDoc.append(SOURCE, annoPage.getSource());
        }

        // don't set translation=false in db, to conserve space
        if (annoPage.isTranslation()) {
            updateDoc.append(TRANSLATION, annoPage.isTranslation());
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
                  // unset deleted field always when we add/update annopage
                 .append(UNSET, new Document(DELETED, "")),
            UPSERT_OPTS);
    }

    public long deprecateAnnoPage(String datasetId, String localId, String pageId, String lang) {
        Instant now = Instant.now();
        return datastore
        .find(AnnoPage.class)
        .filter(
            eq(DATASET_ID, datasetId),
            eq(LOCAL_ID, localId),
            eq(PAGE_ID, pageId),
            eq(LANGUAGE, lang))
        .update(
            set(MODIFIED, now),set(DELETED, now),
            // only remove embedded annotations and Resource
            unset(ANNOTATIONS),
            unset(RESOURCE))
        .execute()
        .getModifiedCount();
    }

    public long deprecateAnnoPages(String datasetId, String localId, String pageId) {
        Instant now = Instant.now();
        return datastore
            .find(AnnoPage.class)
            .filter(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId), eq(PAGE_ID, pageId))
            .update(
                set(MODIFIED, now),set(DELETED, now),
                // only remove embedded annotations and Resource
                unset(ANNOTATIONS),
                unset(RESOURCE))
            .execute(MULTI_UPDATE_OPTS)
            .getModifiedCount();
    }

    /**
     * Gets the AnnoPage with the specified source value
     * @param source source value to query
     * @param fetchFullDoc if false, only dsId, lcId, pgId, tgtId, lang, source and modified values are
     *                     set in the AnnoPage result
     * @return AnnoPage result
     */
    public AnnoPage getAnnoPageWithSource(String source, boolean fetchFullDoc, boolean includeDeprecated) {
        List<Filter> filter =
            new ArrayList<>(
                List.of(eq(SOURCE, source)));

        if(!includeDeprecated){
            filter.add(eq(DELETED, null));
        }

        FindOptions findOptions = new FindOptions().limit(1);
        if (!fetchFullDoc) {
            findOptions =
                findOptions
                    .projection()
                    .include(DATASET_ID, LOCAL_ID, PAGE_ID, TARGET_ID, LANGUAGE, SOURCE, MODIFIED);
        }

        return datastore
            .find(AnnoPage.class)
            .filter(filter.toArray(new Filter[0]))
            .iterator(findOptions)
            .tryNext();
    }

    /**
     * Deprecates the AnnoPage document(s) whose source value is contained within the provided
     * list.
     * @param sources List of sources to be used for AnnoPage deprecation
     * @return number of deprecated documents
     */
    public long deprecateAnnoPagesWithSources(List<? extends String> sources) {
        Instant now = Instant.now();
        return datastore
            .find(AnnoPage.class)
            .filter(in(SOURCE, sources))
            .update(
                set(MODIFIED, now),set(DELETED, now),
                // only remove embedded annotations and Resource
                unset(ANNOTATIONS),
                unset(RESOURCE))
            .execute(MULTI_UPDATE_OPTS)
            .getModifiedCount();
    }

    /** Only for tests */
    public void deleteAll() {
        MorphiaUtils.validateDeletion(activeProfileString);
        datastore.find(AnnoPage.class).delete(MorphiaUtils.MULTI_DELETE_OPTS);
    }

  /**
   * Retrieves a "shell" AnnoPage from the database.
   * TODO: this functionality can be added to {@link #getShellAnnoPageById(String, String, String, String)}~
   * @return
   */
  public AnnoPage getShellAnnoPageById(
      String datasetId, String localId, String pageId, String lang, boolean includeDeprecated) {
    return datastore
        .find(AnnoPage.class)
        .filter(createFilterToGetAnnoPage(datasetId, localId, pageId, lang, includeDeprecated).toArray(new Filter[0]))
        .iterator(
            new FindOptions()
                .limit(1)
                .projection()
                .include(DATASET_ID, LOCAL_ID, PAGE_ID, TARGET_ID, LANGUAGE, SOURCE, MODIFIED, DELETED))
        .tryNext();
  }

    /**
     * Gets the resource ids for AnnoPages matching the sources in the provided list
     * @param sources source url for match AnnoPages with
     * @return list of resource ids
     */
    public List<String> getResourceIdsForAnnoPageSources(List<? extends String> sources) {
        List<AnnoPage> annoPages = datastore.find(AnnoPage.class)
            .filter(in(SOURCE, sources))
            .iterator(new FindOptions().projection().include(RESOURCE))
            .toList();

        return annoPages.stream().map(a -> a.getRes().getId()).collect(Collectors.toList());
    }

  private List<Filter> createFilterToGetAnnoPage(String datasetId, String localId, String pageId, String lang, boolean includeDeprecated) {
      List<Filter> filter =
              new ArrayList<>(
                      Arrays.asList(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId)));

      if (StringUtils.isNotEmpty(pageId)) {
          filter.add(eq(PAGE_ID, pageId));
      }

      if (StringUtils.isNotEmpty(lang)) {
          filter.add(eq(LANGUAGE, lang));
      }

      if (!includeDeprecated) {
          filter.add(eq(DELETED, null));
      }
      return filter;
  }
}
