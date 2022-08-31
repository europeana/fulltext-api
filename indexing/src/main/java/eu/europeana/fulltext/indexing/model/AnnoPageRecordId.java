package eu.europeana.fulltext.indexing.model;

import dev.morphia.annotations.Entity;
import eu.europeana.fulltext.indexing.reader.AnnoPageRecordIdReader;

/** Wrapper for AnnoPage DatasetID and LocalId combination */
@Entity
public class AnnoPageRecordId {
  private final String dsId;
  private final String lcId;

  public AnnoPageRecordId(String dsId, String lcId) {
    this.dsId = dsId;
    this.lcId = lcId;
  }

  public String getDsId() {
    return dsId;
  }

  public String getLcId() {
    return lcId;
  }

  public String toEuropeanaId() {
    return "/" + dsId + "/" + lcId;
  }

  public AnnoPageRecordId copy() {
    return new AnnoPageRecordId(this.dsId, this.lcId);
  }

  @Override
  public String toString() {
    return "AnnoPageRecordId{" + "dsId='" + dsId + '\'' + ", lcId='" + lcId + '\'' + '}';
  }
}
