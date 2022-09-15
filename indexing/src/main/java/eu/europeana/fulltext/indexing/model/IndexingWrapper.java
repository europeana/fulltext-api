package eu.europeana.fulltext.indexing.model;

import java.util.HashSet;
import java.util.Set;
import org.apache.solr.common.SolrInputDocument;

public class IndexingWrapper {

  private final AnnoPageRecordId recordId;
  private final Set<IndexingAction> actions = new HashSet<>();
  private SolrInputDocument solrDocument;

  public IndexingWrapper(AnnoPageRecordId recordId, IndexingAction... actions) {
    this.recordId = recordId;
    this.actions.addAll(Set.of(actions));
  }

  public IndexingWrapper(AnnoPageRecordId recordId, SolrInputDocument document, IndexingAction... actions) {
   this(recordId, actions);
   this.setSolrDocument(document);
  }

  public Set<IndexingAction> getActions() {
    return actions;
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


  public void markForDeletion() {
actions.remove(IndexingAction.WRITE_DOCUMENT);
actions.add(IndexingAction.DELETE_DOCUMENT);
  }

  @Override
  public String toString() {
    return "{"
        + "action="
        + actions
        + ", recordId="
        + recordId
        + ", solrDocument="
        + solrDocument
        + '}';
  }

}
