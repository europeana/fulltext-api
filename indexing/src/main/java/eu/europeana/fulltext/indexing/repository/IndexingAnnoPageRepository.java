package eu.europeana.fulltext.indexing.repository;

import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.stages.Group.id;
import static dev.morphia.query.experimental.filters.Filters.*;
import static dev.morphia.aggregation.experimental.stages.Sort.*;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.DATASET_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.DELETED;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.DOC_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.LANGUAGE;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.LOCAL_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.MODIFIED;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.PAGE_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.RESOURCE;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.TARGET_ID;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.ReplaceRoot;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.Filter;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.indexing.model.AnnoPageRecordId;
import eu.europeana.fulltext.repository.AnnoPageRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.europeana.fulltext.util.MorphiaUtils;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

// TODO - can we not use AnnoPageRepository and add these methods there
@Repository
public class IndexingAnnoPageRepository extends AnnoPageRepository {

  @Value("${spring.profiles.active:}")
  private String activeProfileString;


  private static final List<String> PROJECTION_FIELDS =
      List.of(DATASET_ID, LOCAL_ID, TARGET_ID, LANGUAGE, MODIFIED, RESOURCE, DELETED);

  /**
   * Fetches the records after the date.
   * @param date
   * @return
   */
  public MorphiaCursor<AnnoPage> getRecordsModifiedAfterStream(Date date) {
    return datastore
            .find(AnnoPage.class)
            .filter(gt(MODIFIED, date))
            .iterator(
                    new FindOptions()
                            .projection()
                            .include(DATASET_ID, LOCAL_ID));
  }

  /**
   * Fetches list of AnnoPage matching dsId and lcId
   * Fetches only the ones which are not deprecated or deleted
   * @param dsId
   * @param lcId
   * @return
   */
  public List<AnnoPage> findActiveAnnoPage(String dsId, String lcId) {
    return datastore
            .find(AnnoPage.class)
            .filter(and(eq(DATASET_ID, dsId),eq(LOCAL_ID,lcId)), eq(DELETED, null))
            .iterator(
                    new FindOptions()
                            .projection()
                            .include(DATASET_ID, LOCAL_ID, TARGET_ID, LANGUAGE, MODIFIED, RESOURCE))
            .toList();
  }

  public List<AnnoPage> getAnnoPagesWithProjection(String dsId, String lcId){
    return super.findAnnoPages(dsId, lcId, PROJECTION_FIELDS);
  }

  //not working
  public List<AnnoPage> findAnnoPage(String dsId, String lcId) {
    return datastore
            .find(AnnoPage.class)
            .filter(and(eq(DATASET_ID, dsId),eq(LOCAL_ID,lcId)))
            .iterator(
                    new FindOptions()
                            .projection()
                            .include(DATASET_ID, LOCAL_ID, TARGET_ID, LANGUAGE, MODIFIED, RESOURCE, DELETED))
            .toList();
  }

  public List<AnnoPage> findDeletedAnnoPage(String dsId, String lcId) {
    return datastore
            .find(AnnoPage.class)
            .filter(and(eq(DATASET_ID, dsId),eq(LOCAL_ID,lcId)),  ne(DELETED, null))
            .iterator(
                    new FindOptions()
                            .projection()
                            .include(DATASET_ID, LOCAL_ID, TARGET_ID, LANGUAGE, MODIFIED, RESOURCE))
            .toList();
  }

  /**
   * Checks whether an active record exists for the dsId and lcId combination
   * @param dsId
   * @param lcId
   * @return
   */
  public boolean existsActive(String dsId, String lcId){
    return datastore
        .find(AnnoPage.class)
        .filter(eq(DATASET_ID, dsId),eq(LOCAL_ID,lcId), eq(DELETED, null))
        .count() > 0;
  }

  /**
   * Fetch all the active Annopages
   * @return
   */
  public MorphiaCursor<AnnoPage> findActiveAnnoPage(){
    return datastore.find(AnnoPage.class)
        .filter(eq(DELETED, null))
        .iterator(
            new FindOptions()
                .projection().include(DATASET_ID,LOCAL_ID, MODIFIED));
  }

  /**
   * fetch all the Annopage
   * @return
   */
  public MorphiaCursor<AnnoPage> getAll(){
    return datastore.find(AnnoPage.class)
        .iterator(
            new FindOptions()
                .projection()
                .include(DATASET_ID,LOCAL_ID));
  }


  public MorphiaCursor<AnnoPageRecordId>  getAnnoPageRecordIdByModificationTime(Optional<Instant> from, Instant to) {

    // Aggregatation Stages
    Aggregation<AnnoPage> query = datastore.aggregate(AnnoPage.class);

    List<Filter> match = new ArrayList<>();
    match.add(lt(MODIFIED, to));
    from.ifPresent(instant -> match.add(gt(MODIFIED, instant)));

    query
        .match(match.toArray(new Filter[0]))
        .sort(Sort.sort().ascending(DATASET_ID).ascending(LOCAL_ID).ascending(PAGE_ID).ascending(LANGUAGE))
        .group(
    Group.group(id().field(DATASET_ID).field(LOCAL_ID)))
        .replaceRoot(ReplaceRoot.replaceRoot(field("_id")));

    return query.execute(AnnoPageRecordId.class);
  }
}
