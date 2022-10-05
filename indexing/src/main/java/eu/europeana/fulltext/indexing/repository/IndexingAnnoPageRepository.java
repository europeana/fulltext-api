package eu.europeana.fulltext.indexing.repository;

import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.stages.Group.id;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.gt;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.DATASET_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.DELETED;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.LANGUAGE;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.LOCAL_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.MODIFIED;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.PAGE_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.RESOURCE;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.TARGET_ID;

import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.AggregationOptions;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.ReplaceRoot;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.query.experimental.filters.Filter;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.indexing.model.AnnoPageRecordId;
import eu.europeana.fulltext.repository.AnnoPageRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class IndexingAnnoPageRepository extends AnnoPageRepository {

  private static final AggregationOptions aggregationOpts = new AggregationOptions().allowDiskUse(true);
  private static final List<String> PROJECTION_FIELDS =
      List.of(DATASET_ID, LOCAL_ID, PAGE_ID, TARGET_ID, LANGUAGE, MODIFIED, RESOURCE, DELETED);

  /**
   * Fetches AnnoPages with the given dsId and lcId combination, only populating fields specified in
   * PROJECTION_FIELDS above.
   */
  public List<AnnoPage> getAnnoPagesWithProjection(String dsId, String lcId) {
    return super.findAnnoPages(dsId, lcId, PROJECTION_FIELDS);
  }

  /**
   * Checks whether an active record exists for the dsId and lcId combination
   *
   * @param dsId
   * @param lcId
   * @return
   */
  public boolean existsActive(String dsId, String lcId) {
    return datastore
            .find(AnnoPage.class)
            .filter(eq(DATASET_ID, dsId), eq(LOCAL_ID, lcId), eq(DELETED, null))
            .count()
        > 0;
  }

  /**
   * Gets the record ids (dsId + lcId combination) of AnnoPages modified after the specified timestamp
   *
   * @param from least recent modification timestamp to fetch
   * @return MongoCursor for iterating over results. Callers are responsible for closing the cursor
   */
  public MorphiaCursor<AnnoPageRecordId> getAnnoPageRecordIdByModificationTime(
      Optional<Instant> from) {

    // Aggregation Stages
    Aggregation<AnnoPage> query = datastore.aggregate(AnnoPage.class);

    List<Filter> match = new ArrayList<>();

    // match stage only included if a timestamp is specified, otherwise we aggregate on all records
    // in db
    from.ifPresent(instant -> match.add(gt(MODIFIED, instant)));

    query
        .match(match.toArray(new Filter[0]))
        .sort(Sort.sort().ascending(DATASET_ID).ascending(LOCAL_ID))
        .group(Group.group(id().field(DATASET_ID).field(LOCAL_ID)))
        .replaceRoot(ReplaceRoot.replaceRoot(field("_id")));

    return query.execute(AnnoPageRecordId.class, aggregationOpts);
  }
}
