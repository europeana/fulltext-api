package eu.europeana.fulltext.indexing.repository;

import static dev.morphia.query.experimental.filters.Filters.and;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.gt;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.DATASET_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.DELETED;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.LANGUAGE;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.LOCAL_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.MODIFIED;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.RESOURCE;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.TARGET_ID;

import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaCursor;
import eu.europeana.fulltext.entity.AnnoPage;
import java.util.Date;
import java.util.List;

import eu.europeana.fulltext.util.MorphiaUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

// TODO - can we not use AnnoPageRepository and add these methods there
@Repository
public class IndexingAnnoPageRepository {

  @Value("${spring.profiles.active:}")
  private String activeProfileString;

  @Autowired
  private Datastore datastore;

  public AnnoPage saveAnnoPage(AnnoPage annoPage) {
    return datastore.save(annoPage);
  }

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

  /** Only for tests */
  public void deleteAll() {
    MorphiaUtils.validateDeletion(activeProfileString);
    datastore.find(AnnoPage.class).delete(MorphiaUtils.MULTI_DELETE_OPTS);
  }

}
