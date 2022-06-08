package eu.europeana.fulltext.indexing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.schema.SchemaRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author mmarrero
 * Services to index/synchronize the Solr fulltext collection
 */
public class FulltextCollection {
    private static final Logger logger = LogManager.getLogger(FulltextCollection.class);
    private static final String EUROPEANA_ID = "europeana_id";
    private static final String TIMESTAMP = "timestamp";
    private static final String VERSION = "_version_";
    private static final String TIMESTAMP_UPDATE_METADATA   = "timestamp_update";
    private static final String TIMESTAMP_UPDATE_FULLTEXT   = "timestamp_update_fulltext";
    private static final String FULLTEXT = "fulltext";

    static private SolrClient fulltextSolr;
    static String fulltextCollection;


    /**
     * Retrieves from the Solr metadata collection the list of documents with the ids received and add them to the Solr fulltext collection
     * All fields are copied except for timestamp and _version_
     * @param ids list of europeana_id
     * @throws IOException
     * @throws SolrServerException
     */
    public static void addDocuments(List<String> ids) throws IOException, SolrServerException {
        List<SolrInputDocument> toAdd = new ArrayList<>();
        try {
            for (String europeana_id : ids) {
                toAdd.add(toSolrInputDocument(MetadataCollection.getFullDocument(europeana_id)));
            }
            if (!toAdd.isEmpty()) {
                SolrServices.add(fulltextSolr, fulltextCollection, toAdd);
            }
        } catch (SolrServerException | IOException e){
            logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Remove from the Solr fulltext collection all the documents with the ids received
     * @param ids list of europeana_id
     * @throws IOException
     * @throws SolrServerException
     */
    public static void deleteDocuments(List<String> ids) throws IOException, SolrServerException {
        try {
            SolrServices.deleteById(fulltextSolr,fulltextCollection,ids);
        } catch (SolrServerException | IOException e){
            logger.error(e.getMessage());
            throw e;
        }

    }

    /**
     * Updates the metadata (atomic updates) in the Solr fulltext collection with that in the Solr metadata collection for the ids received
     * @param ids list of europeana_id
     * @throws IOException
     * @throws SolrServerException
     */
    public static void setMetadata(List<String> ids) throws IOException, SolrServerException {
        try {
            List<SolrInputDocument> toSet = new ArrayList<>();
            for (String europeana_id : ids) {
                SolrDocument existingDocument = MetadataCollection.getFullDocument(europeana_id);
                SolrInputDocument newDocument = new SolrInputDocument();
                newDocument.addField(EUROPEANA_ID, europeana_id);
                for (String field : existingDocument.getFieldNames()) {
                    if (!field.equals(TIMESTAMP) && !field.equals(VERSION)) { //_version_ and timestamp are automatically added by Solr
                        Map<String, Object> atomicUpdates = new HashMap<>(1);
                        atomicUpdates.put("set", existingDocument.getFieldValue(field));
                        newDocument.addField(field, atomicUpdates);
                    }
                }
                toSet.add(newDocument);
            }
            SolrServices.add(fulltextSolr, fulltextCollection, toSet);
        } catch (SolrServerException | IOException e){
            logger.error(e.getMessage());
            throw e;
        }
    }

    public static void setFulltext(List<String> ids){
        //update fulltext -by language, and include prefix in contents
        //update timestamp_update_fulltext
        //update issued with normalization of proxy_dcterms_issued?
        //TODO
    }


    /**
     * Return the date and time of the most recent updated metadata, indexed in the field timestamp_update (following the date of update in the metadata collection)
     * @return LocalDateTime
     */
    public static LocalDateTime getLastUpdateMetadata() throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery();
        query.set("q", "*:*");
        query.set("fl", TIMESTAMP_UPDATE_METADATA);
        query.setRows(1);
        query.setSort(TIMESTAMP_UPDATE_METADATA, SolrQuery.ORDER.desc);
        try {
            QueryResponse response = SolrServices.query(fulltextSolr, fulltextCollection, query);
            return LocalDateTime.parse(response.getResults().get(0).getFieldValue(TIMESTAMP_UPDATE_METADATA).toString());
        } catch (IOException | SolrServerException e){
            logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Return the date and time of the most recent updated fulltext, indexed in the field timestamp_update_fulltext (following the date of update in the fulltext MongoDB)
     * @return LocalDateTime
     */
    public static LocalDateTime getLastUpdateFulltext() throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery();
        query.set("q", "*:*");
        query.set("fl", TIMESTAMP_UPDATE_FULLTEXT);
        query.setRows(1);
        query.setSort(TIMESTAMP_UPDATE_FULLTEXT, SolrQuery.ORDER.desc);
        try {
            QueryResponse response = SolrServices.query(fulltextSolr, fulltextCollection, query);
            return LocalDateTime.parse(response.getResults().get(0).getFieldValue(TIMESTAMP_UPDATE_FULLTEXT).toString());
        } catch (IOException | SolrServerException e){
            logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Returns true if the document with europeana_id exists in the collection
     * @param europeana_id
     * @return
     * @throws SolrServerException
     * @throws IOException
     */
    public static boolean exists(String europeana_id) throws SolrServerException, IOException {
        try {
            SolrDocument document = SolrServices.get(fulltextSolr, fulltextCollection, europeana_id);
            if (document != null){ return true;}
            return  false;
        } catch (SolrServerException | IOException e){
            logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Transforms a SolrDocument into a SolrInputDocument so it can be added to a collection
     * All fields are copied except for _version_ and timestamp, which are automatically added by Solr
     * @param document SolrDocument
     * @return
     */
    protected static SolrInputDocument toSolrInputDocument(SolrDocument document) {
        SolrInputDocument inputDocument = new SolrInputDocument();
        for (String field : document.getFieldNames()) {
            if (!field.equals(TIMESTAMP) && !field.equals(VERSION)) { //_version_ and timestamp are automatically added by Solr
                inputDocument.addField(field, document.getFieldValue(field));
            }
        }
        return inputDocument;
    }

    protected static boolean isLangSupported(String language) throws SolrServerException, IOException {
        if (language == null || language.isEmpty())
            return false;
        SchemaRequest request = new SchemaRequest();
        SchemaResponse response = request.process(fulltextSolr, fulltextCollection);
        SchemaRepresentation schema = response.getSchemaRepresentation();
        if (!schema.getFields().stream().map(p -> p.get("name")).collect(Collectors.toList())
                .contains(FULLTEXT + "." + language)) {
            return false;
        }
        return true;
    }

}
