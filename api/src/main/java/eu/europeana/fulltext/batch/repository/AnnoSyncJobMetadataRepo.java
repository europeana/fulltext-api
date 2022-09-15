package eu.europeana.fulltext.batch.repository;


import static eu.europeana.fulltext.AppConstants.FULLTEXT_DATASTORE_BEAN;

import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Sort;
import eu.europeana.fulltext.batch.model.AnnoSyncJobMetadata;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class AnnoSyncJobMetadataRepo {

  private final Datastore datastore;

  public AnnoSyncJobMetadataRepo(@Qualifier(FULLTEXT_DATASTORE_BEAN) Datastore datastore) {
    this.datastore = datastore;
  }

  public AnnoSyncJobMetadata getMostRecentAnnoSyncMetadata() {
    return datastore
        .find(AnnoSyncJobMetadata.class)
        .iterator(new FindOptions().sort(Sort.descending("lastSuccessfulStartTime")).limit(1))
        .tryNext();
  }

  public void save(AnnoSyncJobMetadata metadata) {
    datastore.save(metadata);
  }
}
