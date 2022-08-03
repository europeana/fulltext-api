package eu.europeana.fulltext.indexing;

import static eu.europeana.fulltext.indexing.Constants.METADATA_SOLR_BEAN;

import eu.europeana.fulltext.indexing.service.SolrServices;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.SolrStream;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.params.ModifiableSolrParams;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author mmarrero
 * Services to read the required information from Solr metadata collection
 */
@Component
public class MetadataCollection {
    private static final Logger LOG = LogManager.getLogger(MetadataCollection.class);

    private final CloudSolrClient metadataSolr;
    private String metadataCollectionName;
    //PUT coreURLs heres
    private final List<String> coreURLs;

    public MetadataCollection(
            @Qualifier(METADATA_SOLR_BEAN) CloudSolrClient metadataSolr) throws IOException {
        this.metadataSolr = metadataSolr;
        this.metadataCollectionName = metadataSolr.getDefaultCollection();
        this.coreURLs = getCoreURLs();
    }

    public List<TupleStream> getDocumentsModifiedAfter(ZonedDateTime lastTimestampUpdateMetadata) {
        try {
            List<TupleStream> streams = new ArrayList<>();
            for (String coreURL : coreURLs) { //we have to iterate each core separately
                ModifiableSolrParams params = new ModifiableSolrParams();
                params.set(Constants.SOLR_QUERY, Constants.SOLR_QUERY_DEFAULT);
                params.set(Constants.SOLR_QT, Constants.SOLR_EXPORT);
                params.set(Constants.SOLR_SORT, Constants.EUROPEANA_ID + Constants.SOLR_SORT_ASC);
                params.set(Constants.SOLR_FL, Constants.EUROPEANA_ID);
                String formattedDate = lastTimestampUpdateMetadata.format(DateTimeFormatter.ofPattern(Constants.METADATA_DATE_FORMAT));
                params.set(Constants.SOLR_FQ, Constants.TIMESTAMP_UPDATE_METADATA + ":{" + formattedDate + " TO NOW]");

                TupleStream solrStream = new SolrStream(coreURL, params);
                StreamContext context = new StreamContext();
                solrStream.setStreamContext(context);
                streams.add(solrStream);
            }
            return streams;
        }catch (IndexOutOfBoundsException  e){
            LOG.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves the list of ids of the documents modified (timestamp_update) after last_timestamp_update_metadata
     * @param  streams
     * @return list of europeana_id
     * @throws IOException
     */
    public List<String> getDocumentsModifiedAfter(List<TupleStream> streams) throws IOException {
        List<String> ids = new ArrayList<>();
        for (TupleStream solrStream: streams) { //we have to iterate each core separately
            try {
                solrStream.open();
                Tuple tuple = solrStream.read();
                while (!tuple.EOF) {
                    ids.add(tuple.getString(Constants.EUROPEANA_ID));
                    tuple = solrStream.read();
                }
            } catch (IOException e) {
                LOG.error("Error getting the list of ids of the document - {} ", e.getMessage());
                throw e;
            } finally {
                solrStream.close();
            }
        }
        return ids;
    }

    /**
     * Returns the date of the last update of the metadata (timestamp_update)
     * @param europeanaId
     * @return ZonedDateTime with last update, null if document does not exists
     * @throws SolrServerException
     * @throws IOException
     */
    public ZonedDateTime getLastUpdateDate(String europeanaId) throws SolrServerException, IOException {
        try {
            SolrDocument document = SolrServices.get(metadataSolr, metadataCollectionName, europeanaId);
            if (document != null) {
                return  ((Date)document.getFieldValue(Constants.TIMESTAMP_UPDATE_METADATA)).toInstant()
                        .atZone(ZoneOffset.UTC); //dates in Solr are always in format ISO8601 and UTC

            }
            LOG.info("No records in the metadata collection");
            return null;
        } catch (SolrServerException | IOException e){
            LOG.error("Error getting the timestamp_update of the metadata - {}", e.getMessage());
            throw e;
        }
    }



    /**
     * Retrieves the document with the id received from the Solr metadata collection. All fields are retrieved
     * @param europeanaId
     * @throws NullPointerException if document does not exist
     * @return
     *
     */
    protected SolrDocument getDocument(String europeanaId) throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery();
        query.set(Constants.SOLR_QUERY, Constants.EUROPEANA_ID + ":\"" + europeanaId + "\"");
        query.set(Constants.SOLR_FL, Constants.ALL); //retrieve all the fields
        try {
            QueryResponse response = SolrServices.query(metadataSolr, metadataCollectionName, query);
            if (response != null && response.getResults().size() > 0){
                return response.getResults().get(0);
            }
            return  null;
        } catch (IOException | SolrServerException  e){
            LOG.error("Error retrieving record {} - {} " , europeanaId , e.getMessage());
            throw e;
        }
    }

    private List<String> getCoreURLs() throws IOException {
        try {
            List<String> coreList = new ArrayList<String>();
            for (Replica replica : metadataSolr.getClusterStateProvider().getClusterState().getCollection(metadataCollectionName).getReplicas()) {
                String baseUrl = replica.getBaseUrl();
                String coreName = replica.getCoreName();
                coreList.add(baseUrl + "/" + coreName);
            }
            return coreList;
        } catch (IOException e){
            LOG.error(e.getMessage());
            throw e;
        }
    }
}
