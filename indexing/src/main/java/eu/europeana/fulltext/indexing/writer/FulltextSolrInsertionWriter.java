package eu.europeana.fulltext.indexing.writer;

import eu.europeana.fulltext.indexing.model.IndexingAction;
import eu.europeana.fulltext.indexing.model.IndexingWrapper;
import eu.europeana.fulltext.indexing.solr.FulltextSolrService;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class FulltextSolrInsertionWriter implements ItemWriter<IndexingWrapper> {

  private final FulltextSolrService solrService;

  public FulltextSolrInsertionWriter(FulltextSolrService solrService) {
    this.solrService = solrService;
  }

  @Override
  public void write(List<? extends IndexingWrapper> list) throws Exception {
    // we only write SolrInputDocuments if action is "create" or "update"
    List<SolrInputDocument> docsToWrite =
        list.stream()
            .filter(
                w ->
                    w.getAction().equals(IndexingAction.CREATE)
                        || w.getAction().equals(IndexingAction.UPDATE))
            .map(IndexingWrapper::getSolrDocument)
            .collect(Collectors.toList());

    solrService.writeToSolr(docsToWrite);
  }
}
