package eu.europeana.fulltext.indexing;

import static eu.europeana.fulltext.indexing.Constants.METADATA_SOLR_BEAN;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.SolrStream;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author mmarrero
 * Services to read the required information from Solr metadata collection
 */
@Component
public class MetadataCollection {
    private final SolrClient metadataSolr;

    private  final Logger logger = LogManager.getLogger(MetadataCollection.class);
    private final String EUROPEANA_ID = "europeana_id";
    private  final String TIMESTAMP_UPDATE_METADATA   = "timestamp_update";
    private String metadataCollectionName ="search_production_publish_4"; //TODO: include as property


    //PUT coreURLs heres
    private static final List<String> coreURLs = List.of(); //TODO: better to get them from the Solr instance, like now? if not static, we can call getCoreNames()

    public MetadataCollection(
        @Qualifier(METADATA_SOLR_BEAN) SolrClient metadataSolr) {
        this.metadataSolr = metadataSolr;
    }

    /**
     * Retrieves the list of ids of the documents modified (timestamp_update) after last_timestamp_update_metadata
     * @param last_timestamp_update_metadata
     * @return list of europeana_id
     * @throws IOException
     */
    public List<String> getDocumentsModifiedAfter(LocalDateTime last_timestamp_update_metadata) throws IOException, SolrServerException {
        try {
            List<String> ids = new ArrayList<>();
            for (String coreURL : getCoreNames()) { //we have to iterate each core separately
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
        }catch (IOException | SolrServerException e){
            logger.error(e.getMessage());
            throw e;
        }
    }
    /**
     * Returns the date of the last update of the metadata (timestamp_update)
     * @param europeana_id
     * @return LocalDateTime with last update, null if document does not exists
     * @throws SolrServerException
     * @throws IOException
     */
    public LocalDateTime getLastUpdateDate(String europeana_id) throws SolrServerException, IOException {
        try {
            SolrDocument document = SolrServices.get(metadataSolr, metadataCollectionName, europeana_id);
            if (document != null) {
                return  ((Date)document.getFieldValue(TIMESTAMP_UPDATE_METADATA)).toInstant()
                        .atZone(ZoneOffset.UTC)
                        .toLocalDateTime();
            }
            return null;
        } catch (SolrServerException | IOException e){
            logger.error(e.getMessage());
            throw e;
        }
    }



    /**
     * Retrieves the document with the id received from the Solr metadata collection. All fields are retrieved
     * @param europeana_id
     * @throws NullPointerException if document does not exist
     * @return
     *
     */
    protected SolrDocument getDocument(String europeana_id) throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery();
        query.set("q", EUROPEANA_ID + ":\"" + europeana_id + "\"");
        query.set("fl", "*"); //retrieve all the fields
        try {
            QueryResponse response = SolrServices.query(metadataSolr, metadataCollectionName, query);
            return response.getResults().get(0);
        } catch (IOException | SolrServerException | NullPointerException e){
            logger.error("Error retrieving record " + europeana_id + " - " + e.getMessage());
            throw e;
        }
    }

    protected List<String> getCoreNames() throws IOException, SolrServerException {
        try {
            List<String> coreList = new ArrayList<String>();
            CoreAdminRequest request = new CoreAdminRequest();
            request.setAction(CoreAdminParams.CoreAdminAction.STATUS);
            CoreAdminResponse cores = request.process(metadataSolr);
            for (int i = 0; i < cores.getCoreStatus().size(); i++) {
                coreList.add(cores.getCoreStatus().getName(i));
            }
            return coreList;
        }catch (SolrServerException | IOException e){
            logger.error(e.getMessage());
            throw e;
        }
    }


}
