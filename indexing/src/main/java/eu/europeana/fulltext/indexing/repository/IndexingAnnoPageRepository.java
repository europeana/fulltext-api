package eu.europeana.fulltext.indexing.repository;

import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.stages.Group.group;
import static dev.morphia.aggregation.experimental.stages.Group.id;
import static dev.morphia.query.experimental.filters.Filters.and;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.exists;
import static dev.morphia.query.experimental.filters.Filters.gt;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.DATASET_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.DELETED;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.DOC_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.LANGUAGE;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.LOCAL_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.MODIFIED;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.RESOURCE;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.TARGET_ID;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.aggregation.experimental.stages.ReplaceRoot;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaCursor;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.indexing.DataIdWrapper;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.xml.crypto.Data;

@Repository
public class IndexingAnnoPageRepository {

  @Autowired private Datastore datastore;


  /**
   * Fetches all records modified after the specified date.
   * Returns a list containing the unique dsId and lcId combinations from modified AnnoPages.
   */
/*  public List<DataIdWrapper> getRecordsModifiedAfter(Date date) {
    return datastore
        .aggregate(AnnoPage.class)
        //.match(gt(MODIFIED, date), eq(DELETED, null)) Replacing this one by the one below because we also need to know if there were modifications
        .match(gt(MODIFIED, date))
        .project(Projection.project().include(DATASET_ID).include(LOCAL_ID))
        .group(group(id().field(DATASET_ID).field(LOCAL_ID)))
        // _id created in group stage, containing dsId and lcId. We
        .replaceRoot(ReplaceRoot.replaceRoot(field(DOC_ID)))
        .execute(DataIdWrapper.class)
        .toList();
  }*/

  //this seems to be more efficient if we try to get big amounts of records
  public MorphiaCursor<AnnoPage> getRecordsModifiedAfter_stream(Date date) {
    return datastore
            .find(AnnoPage.class)
            //.match(gt(MODIFIED, date), eq(DELETED, null)) Replacing this one by the one below because we also need to know if there were modifications
            .filter(gt(MODIFIED, date))
            .iterator(
                    new FindOptions()
                            .projection()
                            .include(DATASET_ID, LOCAL_ID));
  }


    public List<AnnoPage> getActive(String dsId, String lcId) {
    return datastore
            .find(AnnoPage.class)
            .filter(and(eq(DATASET_ID, dsId),eq(LOCAL_ID,lcId)), eq(DELETED, null))
            .iterator(
                    new FindOptions()
                            .projection()
                            .include(DATASET_ID, LOCAL_ID, TARGET_ID, LANGUAGE, MODIFIED, RESOURCE))
            .toList();
  }


  /**
   * Checks whether an active record exists for the dsId and lcId combination
   */
  public boolean existsActive(String dsId, String lcId){
    return datastore
        .find(AnnoPage.class)
        .filter(eq(DATASET_ID, dsId),eq(LOCAL_ID,lcId), eq(DELETED, null))
        .count() > 0;
  }


  public MorphiaCursor<AnnoPage> getActive(){
    return datastore.find(AnnoPage.class)
        .filter(eq(DELETED, null))
        .iterator(
            new FindOptions()
                .projection().include(DATASET_ID,LOCAL_ID, MODIFIED));
  }

  public MorphiaCursor<AnnoPage> getAll(){
    return datastore.find(AnnoPage.class)
        .iterator(
            new FindOptions()
                .projection()
                .include(DATASET_ID,LOCAL_ID));
  }

}
