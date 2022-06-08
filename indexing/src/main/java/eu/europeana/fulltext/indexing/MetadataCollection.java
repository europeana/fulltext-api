package eu.europeana.fulltext.indexing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.SolrStream;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.ModifiableSolrParams;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author mmarrero
 * Services to read the required information from Solr metadata collection
 */
public class MetadataCollection {
    private static final Logger logger = LogManager.getLogger(MetadataCollection.class);
    private static final String EUROPEANA_ID = "europeana_id";
    private static final String TIMESTAMP_UPDATE_METADATA   = "timestamp_update";
    static private SolrClient metadataSolr;
    static String metadataCollection;


    /**
     * Retrieves the list of ids of the documents modified (timestamp_update) after last_timestamp_update_metadata
     * @param last_timestamp_update_metadata
     * @return list of europeana_id
     * @throws IOException
     */
    public static List<String> getDocumentsModifiedAfter(LocalDateTime last_timestamp_update_metadata) throws IOException {
        try {
            List<String> ids = new ArrayList<>();
            for (String coreURL : coreURLs) { //we have to iterate each core separately
                ModifiableSolrParams params = new ModifiableSolrParams();
                params.set("q", "*:*");
                params.set("qt", "/export");
                params.set("sort", EUROPEANA_ID + " asc");
                params.set("fl", EUROPEANA_ID);
                params.set("fq", TIMESTAMP_UPDATE_METADATA + ":{" + last_timestamp_update_metadata.toString() + " TO NOW]");

                TupleStream solrStream = new SolrStream(coreURL, params);
                StreamContext context = new StreamContext();
                solrStream.setStreamContext(context);
                try {
                    solrStream.open();
                    Tuple tuple = solrStream.read();
                    while (!tuple.EOF) {
                        ids.add(tuple.getString(EUROPEANA_ID));
                        tuple = solrStream.read();
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage());
                } finally {
                    solrStream.close();
                }
            }
            return ids;
        }catch (IOException e){
            logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves the document with the id received from the Solr metadata collection. All fields are retrieved
     * @param europeana_id
     * @return
     */
    public static SolrDocument getFullDocument(String europeana_id) throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery();
        query.set("q", EUROPEANA_ID + ":\"" + europeana_id + "\"");
        query.set("fl", "*"); //retrieve all the fields
        try {
            QueryResponse response = SolrServices.query(metadataSolr, metadataCollection, query);
            return response.getResults().get(0);
        } catch (IOException | SolrServerException e){
            logger.error("Error retrieving record " + europeana_id + " - " + e.getMessage());
            throw e;
        }
    }


}
