package eu.europeana.fulltext.indexing.utils;

import org.apache.solr.client.solrj.SolrServerException;

import java.io.IOException;
import java.util.List;

public interface BatchProcessor<T> {
    public void process(List<T> list) throws IOException, SolrServerException;
}
