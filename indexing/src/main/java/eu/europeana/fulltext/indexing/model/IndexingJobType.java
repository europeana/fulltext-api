package eu.europeana.fulltext.indexing.model;

public enum IndexingJobType {
  FULLTEXT_INDEXING("fulltext_indexing"),
  METADATA_SYNC("metadata_sync");

  final String value;

  IndexingJobType(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

}
