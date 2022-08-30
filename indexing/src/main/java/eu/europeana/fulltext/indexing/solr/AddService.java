package eu.europeana.fulltext.indexing.solr;

import eu.europeana.fulltext.indexing.batch.BatchProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.util.List;

public class AddService implements BatchProcessor<SolrInputDocument> {
    private static final Logger logger = LogManager.getLogger(AddService.class);

    private SolrClient client;
    private String collection;

    public AddService(SolrClient client, String collection){
        this.client = client;
        this.collection = collection;
    }

    @Override
    public void process(List<SolrInputDocument> list) throws IOException, SolrServerException {
        logger.info("Updating/adding " + list.size() + " records");
        SolrServices.add(client,collection,list);
    }
}
