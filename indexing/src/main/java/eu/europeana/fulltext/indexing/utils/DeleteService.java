package eu.europeana.fulltext.indexing.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;

import java.io.IOException;
import java.util.List;

public class DeleteService implements BatchProcessor<String> {
    private static final Logger logger = LogManager.getLogger(DeleteService.class);

    private SolrClient client;
    private String collection;

    public DeleteService(SolrClient client, String collection){
        this.client = client;
        this.collection = collection;
    }

    @Override
    public void process(List<String> list) throws IOException, SolrServerException {
        logger.info("Deleting " + list.size() + " records");
        SolrServices.deleteById(client,collection,list);
    }
}
