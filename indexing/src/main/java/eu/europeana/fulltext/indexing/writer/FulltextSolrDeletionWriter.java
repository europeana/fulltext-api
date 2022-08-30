package eu.europeana.fulltext.indexing.writer;

import eu.europeana.fulltext.indexing.batch.IndexingAction;
import eu.europeana.fulltext.indexing.batch.IndexingWrapper;
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
    // For this writer, we only handle records flagged with the "Delete" action

    List<String> europeanaIds =
        list.stream()
            .filter(w -> w.getAction().equals(IndexingAction.DELETE))
            .map(w -> w.getRecordId().toEuropeanaId())
            .collect(Collectors.toList());

    fulltextSolr.deleteFromSolr(europeanaIds);
  }
}
