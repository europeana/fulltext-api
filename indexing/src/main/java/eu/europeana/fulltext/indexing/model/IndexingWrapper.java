package eu.europeana.fulltext.indexing.model;

import org.apache.solr.common.SolrInputDocument;

public class IndexingWrapper {

  private final IndexingAction action;
  private final AnnoPageRecordId recordId;

  private SolrInputDocument solrDocument;

  public IndexingWrapper(IndexingAction action, AnnoPageRecordId recordId) {
    this.action = action;
    this.recordId = recordId;
  }

  public IndexingAction getAction() {
    return action;
  }

  public AnnoPageRecordId getRecordId() {
    return recordId;
  }

  public SolrInputDocument getSolrDocument() {
    return solrDocument;
  }

  public void setSolrDocument(SolrInputDocument solrDocument) {
    this.solrDocument = solrDocument;
  }

  @Override
  public String toString() {
    return "IndexingWrapper{" +
        "action=" + action +
        ", recordId=" + recordId +
        ", solrDocument=" + solrDocument +
        '}';
  }
}
