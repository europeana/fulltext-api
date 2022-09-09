package eu.europeana.fulltext.indexing.writer;

import eu.europeana.fulltext.indexing.model.IndexingAction;
import eu.europeana.fulltext.indexing.model.IndexingWrapper;
import eu.europeana.fulltext.indexing.solr.FulltextSolrService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class FulltextSolrDeletionWriter implements ItemWriter<IndexingWrapper> {

  private final FulltextSolrService fulltextSolr;

  public FulltextSolrDeletionWriter(FulltextSolrService fulltextSolr) {
    this.fulltextSolr = fulltextSolr;
  }

  @Override
  public void write(List<? extends IndexingWrapper> list) throws Exception {
    List<String> europeanaIds =
        list.stream()
            .filter(w -> w.getActions().contains(IndexingAction.DELETE_DOCUMENT))
            .map(w -> w.getRecordId().toEuropeanaId())
            .collect(Collectors.toList());

    if (!europeanaIds.isEmpty()) {
      fulltextSolr.deleteFromSolr(europeanaIds);
    }
  }
}
