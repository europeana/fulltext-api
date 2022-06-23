package eu.europeana.fulltext.indexing;

import dev.morphia.annotations.Entity;

/**
 * Wrapper class for database queries that only fetch dsId and lcId fields from AnnoPages
 */
@Entity
public class DataIdWrapper {

  private final String dsId;
  private final String lcId;

  public DataIdWrapper(String dsId, String lcId) {
    this.dsId = dsId;
    this.lcId = lcId;
  }

  public String getDsId() {
    return dsId;
  }

  public String getLcId() {
    return lcId;
  }
}
