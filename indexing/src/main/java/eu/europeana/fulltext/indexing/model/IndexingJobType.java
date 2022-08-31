package eu.europeana.fulltext.indexing.model;

import java.util.Arrays;

public enum IndexingJobType {
  FULLTEXT_INDEXING("fulltext_indexing"),
  METADATA_SYNC("metadata_sync");

  final String value;

  IndexingJobType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static IndexingJobType getTypeByValue(String value) {
    return Arrays.stream(IndexingJobType.values())
        .filter(type -> type.value.equalsIgnoreCase(value))
        .findFirst()
        .orElse(null);
  }
}
