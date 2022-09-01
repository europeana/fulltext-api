package eu.europeana.fulltext.indexing.model;

import org.apache.solr.common.SolrInputDocument;

public class IndexingWrapper {

  private IndexingAction action;
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


  public void setAction(IndexingAction action) {
    this.action = action;
  }

  @Override
  public String toString() {
    return "{"
        + "action="
        + action
        + ", recordId="
        + recordId
        + ", solrDocument="
        + solrDocument
        + '}';
  }

}
