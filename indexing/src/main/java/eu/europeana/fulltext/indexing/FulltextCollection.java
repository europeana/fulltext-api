package eu.europeana.fulltext.indexing;

import static eu.europeana.fulltext.indexing.Constants.FULLTEXT_SOLR_BEAN;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.apache.solr.common.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author mmarrero
 * Services to index/synchronize the Solr fulltext collection
 */
@Component
public class FulltextCollection {
    private static final Logger logger = LogManager.getLogger(FulltextCollection.class);
    private static final String EUROPEANA_ID = "europeana_id";
    private static final String TIMESTAMP = "timestamp";
    private static final String VERSION = "_version_";
    private static final String TIMESTAMP_UPDATE_METADATA   = "timestamp_update";
    private static final String TIMESTAMP_UPDATE_FULLTEXT   = "timestamp_update_fulltext";
    private static final String FULLTEXT = "fulltext";
    private static final String PROXY_ISSUED = "proxy_dcterms_issued";
    private static final String ISSUED = "issued";
    private final SolrClient fulltextSolr;
    private final MetadataCollection metadataCollection;
    private static String fulltextCollectionName ="fulltext"; //TODO API: maybe include as property


    @Autowired
    private IndexingAnnoPageRepository repository;

    public FulltextCollection(
        @Qualifier(FULLTEXT_SOLR_BEAN) SolrClient fulltextSolr,
        MetadataCollection metadataCollection) {
        this.fulltextSolr = fulltextSolr;
        this.metadataCollection = metadataCollection;
    }

/*
    public FulltextCollection(String[] solrURLs, String solrCollection){
        CloudSolrClient client = new CloudSolrClient.Builder(Arrays.asList(solrURLs)).build();
        client.setDefaultCollection(solrCollection);
        this.solrClient = client;
        this.solrCollection = solrCollection;
    }*/

    /**
     * Retrieves from the Solr metadata collection the list of documents with the ids received and add them to the Solr fulltext collection
     * Only id is included
     * @param ids list of europeana_id
     * @throws IOException
     * @throws SolrServerException
     */
    public void addDocuments(List<String> ids, MetadataCollection metadataCollection) throws IOException, SolrServerException {
        List<SolrInputDocument> toAdd = new ArrayList<>();
        try {
            for (String europeana_id : ids) {
                //toAdd.add(toSolrInputDocument(metadataCollection.getFullDocument(europeana_id)));
                SolrInputDocument newDocument = new SolrInputDocument();
                newDocument.addField(EUROPEANA_ID, europeana_id);
                toAdd.add(newDocument);
            }
            if (!toAdd.isEmpty()) {
                SolrServices.add(fulltextSolr, fulltextCollectionName, toAdd);
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
    public void deleteDocuments(List<String> ids) throws IOException, SolrServerException {
        try {
            SolrServices.deleteById(fulltextSolr, fulltextCollectionName,ids);
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
    public void setMetadata(List<String> ids, MetadataCollection metadataCollection) throws IOException, SolrServerException {
        try {
            List<SolrInputDocument> toSet = new ArrayList<>();
            for (String europeana_id : ids) {
                SolrDocument existingDocument = metadataCollection.getFullDocument(europeana_id);
                SolrInputDocument newDocument = new SolrInputDocument();
                newDocument.addField(EUROPEANA_ID, europeana_id);
                for (String field : existingDocument.getFieldNames()) {
                    if (field.equals(PROXY_ISSUED)) {
                        Collection<Object> list_issued_dates = existingDocument.getFieldValues(PROXY_ISSUED);
                        List<String> iso_dates = new ArrayList<>();
                        for (Object d : list_issued_dates) {

                            LocalDate localDate = LocalDate.parse(d.toString());
                            ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.of("Z"));
                            iso_dates.add(zonedDateTime.format(DateTimeFormatter.ISO_INSTANT));

                            if (!iso_dates.isEmpty()) {
                                Map<String, Object> atomicUpdates = new HashMap<>(1);
                                atomicUpdates.put("set", iso_dates);
                                newDocument.addField(ISSUED, atomicUpdates);
                            }
                        }

                    } else if (!field.equals(TIMESTAMP) && !field.equals(VERSION)) { //_version_ and timestamp are automatically added by Solr
                        Map<String, Object> atomicUpdates = new HashMap<>(1);
                        atomicUpdates.put("set", existingDocument.getFieldValue(field));
                        newDocument.addField(field, atomicUpdates);
                    }
                }

                toSet.add(newDocument);
            }
            SolrServices.add(fulltextSolr, fulltextCollectionName, toSet);
        } catch (SolrServerException | IOException e){
            logger.error(e.getMessage());
            throw e;
        }

    }

    /**
     * Updates the fulltext (atomic updates) in the Solr fulltext collection with that in fulltext MongoDB for the ids received
     * @param ids list of europeana_id
     * @throws IOException
     * @throws SolrServerException
     */
    public void setFulltext(List<String> ids) throws IOException, SolrServerException {
        //TODO> update issued with normalization of proxy_dcterms_issued

        List<SolrInputDocument> toSet = new ArrayList<>();
        for (String europeana_id : ids) {
            Map<String,List<String>> lang_ftcontent = new HashMap<>();
            SolrInputDocument newDocument = new SolrInputDocument();
            newDocument.addField(EUROPEANA_ID, europeana_id);
            List<AnnoPage> list_ap = repository.getActive(getDsId(europeana_id), getLcId(europeana_id));
            Date modified = Date.from(Instant.MIN);
            for (AnnoPage ap: list_ap) {
                String fulltext = ap.getRes().getValue();
                String lang = ap.getLang();
                String target = ap.getTgtId();
                Date ap_modified = ap.getModified();
                if (modified.after(ap_modified)){
                    modified = ap_modified;
                }
                String content = addFulltextPrefix(target,fulltext);
                List<String> list_contents = lang_ftcontent.get(lang);
                if (list_contents ==  null){
                    list_contents = new ArrayList<>();
                    lang_ftcontent.put(lang,list_contents);
                }
                list_contents.add(content);
            }
            for (String lang: lang_ftcontent.keySet()){
                Map<String, Object> atomicUpdates = new HashMap<>(1);
                atomicUpdates.put("set", lang_ftcontent.get(lang));
                newDocument.addField(FULLTEXT + "." + lang, atomicUpdates);
            }
            Map<String, Object> atomicUpdates = new HashMap<>(1);
            atomicUpdates.put("set", modified);
            newDocument.addField(TIMESTAMP_UPDATE_FULLTEXT, atomicUpdates);
            toSet.add(newDocument);
        }
        if (!toSet.isEmpty()) {
            try {
                SolrServices.add(fulltextSolr, fulltextCollectionName, toSet);
            } catch (SolrServerException | IOException e) {
                logger.error(e.getMessage());
                throw e;
            }
        }

    }

    /**
     * Prefix added to each fulltext content. It will be stored but not indexed. It is useful for the API to locate the matched terms and highlight them in the image (if applicable)
     * @param fulltext
     * @param target
     * @return
     */
    public String addFulltextPrefix(String fulltext, String target) {
        return "{" + target + "} " + fulltext;
    }


    /**
     * Return the date and time of the most recent updated metadata, indexed in the field timestamp_update (following the date of update in the metadata collection)
     * @return LocalDateTime
     */
    public LocalDateTime getLastUpdateMetadata() throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery();
        query.set("q", "*:*");
        query.set("fl", TIMESTAMP_UPDATE_METADATA);
        query.setRows(1);
        query.setSort(TIMESTAMP_UPDATE_METADATA, SolrQuery.ORDER.desc);
        try {
            QueryResponse response = SolrServices.query(fulltextSolr, fulltextCollectionName, query);
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
    public LocalDateTime getLastUpdateFulltext() throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery();
        query.set("q", "*:*");
        query.set("fl", TIMESTAMP_UPDATE_FULLTEXT);
        query.setRows(1);
        query.setSort(TIMESTAMP_UPDATE_FULLTEXT, SolrQuery.ORDER.desc);
        try {
            QueryResponse response = SolrServices.query(fulltextSolr, fulltextCollectionName, query);
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
    public boolean exists(String europeana_id) throws SolrServerException, IOException {
        try {
            SolrDocument document = SolrServices.get(fulltextSolr, fulltextCollectionName, europeana_id);
            if (document != null){ return true;}
            return  false;
        } catch (SolrServerException | IOException e){
            logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Returns the date of the last update of the metadata and fulltext content, in that order
     * @param europeana_id
     * @return Pair<LocalDateTime,LocalDateTime> with last updates, null if document does not exists
     * @throws SolrServerException
     * @throws IOException
     */
    public Pair<LocalDateTime, LocalDateTime> getLastUpdateDates(String europeana_id) throws SolrServerException, IOException {
        try {
            SolrDocument document = SolrServices.get(fulltextSolr, fulltextCollectionName, europeana_id);
            if (document != null) {
                return  new Pair<>(LocalDateTime.parse(document.getFieldValue(TIMESTAMP_UPDATE_METADATA).toString()), LocalDateTime.parse(document.getFieldValue(TIMESTAMP_UPDATE_FULLTEXT).toString()));
            }
            return null;
        } catch (SolrServerException | IOException e){
            logger.error(e.getMessage());
            throw e;
        }
    }

    public static String getEuropeanaId(String dsId, String lcId){ //maybe this logic is implemented already somewhere in the project
        return "/" + dsId +"/" + lcId;
    }

    public static String getDsId(String europeana_id){
        String[] parts = europeana_id.split("/");
        return parts[0];
    }

    public static String getLcId(String europeana_id){
        String[] parts = europeana_id.split("/");
        return parts[1];
    }


    /**
     * Transforms a SolrDocument into a SolrInputDocument so it can be added to a collection
     * All fields are copied except for _version_ and timestamp, which are automatically added by Solr
     * @param document SolrDocument
     * @return
     */
    protected SolrInputDocument toSolrInputDocument(SolrDocument document) {
        SolrInputDocument inputDocument = new SolrInputDocument();
        for (String field : document.getFieldNames()) {
            if (!field.equals(TIMESTAMP) && !field.equals(VERSION)) { //_version_ and timestamp are automatically added by Solr
                inputDocument.addField(field, document.getFieldValue(field));
            }
        }
        return inputDocument;
    }

    protected boolean isLangSupported(String language) throws SolrServerException, IOException {
        if (language == null || language.isEmpty())
            return false;
        SchemaRequest request = new SchemaRequest();
        SchemaResponse response = request.process(fulltextSolr, fulltextCollectionName);
        SchemaRepresentation schema = response.getSchemaRepresentation();
        if (!schema.getFields().stream().map(p -> p.get("name")).collect(Collectors.toList())
                .contains(FULLTEXT + "." + language)) {
            return false;
        }
        return true;
    }



}
