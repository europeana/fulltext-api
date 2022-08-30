package eu.europeana.fulltext.indexing.batch;

import eu.europeana.fulltext.indexing.model.AnnoPageRecordId;
import org.apache.solr.common.SolrDocument;
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
}
