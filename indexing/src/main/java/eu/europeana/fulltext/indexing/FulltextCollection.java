package eu.europeana.fulltext.indexing;

import static eu.europeana.fulltext.indexing.Constants.FULLTEXT_SOLR_BEAN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
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
import java.util.stream.Collectors;

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
    private static final String IS_FULLTEXT = "is_fulltext";
    private final CloudSolrClient fulltextSolr;
    private final MetadataCollection metadataCollection;
    private final String fulltextCollectionName;


    @Autowired
    private IndexingAnnoPageRepository repository;

    public FulltextCollection(
        @Qualifier(FULLTEXT_SOLR_BEAN) CloudSolrClient fulltextSolr,
        MetadataCollection metadataCollection) {
        this.fulltextSolr = fulltextSolr;
        this.fulltextCollectionName = fulltextSolr.getDefaultCollection();
        this.metadataCollection = metadataCollection;
    }

    /**
     * Retrieves from the Solr metadata collection the list of documents with the ids received and add them to the Solr fulltext collection
     * Only id is included
     * @param ids list of europeana_id
     * @throws IOException
     * @throws SolrServerException
     */
/*    public void addDocuments(List<String> ids, MetadataCollection metadataCollection) throws IOException, SolrServerException {
        List<SolrInputDocument> toAdd = new ArrayList<>();
        try {
            for (String europeana_id : ids) {
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
    }*/

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
     * If the document does not exist, it will be created
     * @param ids list of europeana_id
     * @throws IOException
     * @throws SolrServerException
     */
    public void setMetadata(List<String> ids, MetadataCollection metadataCollection) throws IOException, SolrServerException {
        try {
            List<SolrInputDocument> toSet = new ArrayList<>();

            for (String europeana_id : ids) { //TODO: we may improve efficiency by adding threads here
                SolrDocument existingDocument = metadataCollection.getDocument(europeana_id);
                SolrInputDocument newDocument = new SolrInputDocument();
                newDocument.addField(EUROPEANA_ID, europeana_id);
                for (String field : existingDocument.getFieldNames()) {
                    if (field.equals(PROXY_ISSUED)) {
                        Collection<Object> list_issued_dates = existingDocument.getFieldValues(PROXY_ISSUED);
                        List<String> iso_dates = new ArrayList<>();
                        for (Object d : list_issued_dates) {
                            try {
                                LocalDate localDate = LocalDate.parse(d.toString());
                                ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.of("Z"));
                                iso_dates.add(zonedDateTime.format(DateTimeFormatter.ISO_INSTANT));
                            } catch (DateTimeException e) {
                                logger.info("Not parsable date in record  " + europeana_id + ":" + d.toString());
                            }
                            if (!iso_dates.isEmpty()) {
                                Map<String, Object> atomicUpdates = new HashMap<>(1);
                                atomicUpdates.put("set", iso_dates);
                                newDocument.addField(ISSUED, atomicUpdates);
                            }
                        }
                    }
                    if (field.equals(IS_FULLTEXT)){
                        Map<String, Object> atomicUpdates = new HashMap<>(1);
                        atomicUpdates.put("set", true);
                        newDocument.addField(field, atomicUpdates);
                    }
                    if (!field.equals(EUROPEANA_ID) && !field.equals(TIMESTAMP) && !field.equals(VERSION) && !field.equals(IS_FULLTEXT)) { //_version_ and timestamp are automatically added by Solr
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
     * If the document does not exist, it will be created
     * @param ids list of europeana_id
     * @throws IOException
     * @throws SolrServerException
     */
    public void setFulltext(List<String> ids) throws IOException, SolrServerException {
        List<SolrInputDocument> toSet = new ArrayList<>();
        for (String europeana_id : ids) { //TODO: we may improve efficiency by adding threads here
            Map<String,List<String>> lang_ftcontent = new HashMap<>();
            SolrInputDocument newDocument = new SolrInputDocument();
            newDocument.addField(EUROPEANA_ID, europeana_id);
            List<AnnoPage> list_ap = repository.getActive(getDsId(europeana_id), getLcId(europeana_id));
            Date modified = Date.from(Instant.EPOCH);
            for (AnnoPage ap: list_ap) {
                String fulltext = ap.getRes().getValue();
                String lang = ap.getLang();
                if (!isLangSupported(lang)){
                    logger.warn("Record "+ europeana_id+"-language not supported: "+ lang +". Indexing in fulltext.");
                    lang ="";
                }
                String target = ap.getTgtId();
                Date ap_modified = ap.getModified();
                if (modified.before(ap_modified)) {
                    modified = ap_modified;
                }
                String content = addFulltextPrefix(target, fulltext);
                List<String> list_contents = lang_ftcontent.get(lang);
                if (list_contents == null) {
                    list_contents = new ArrayList<>();
                    lang_ftcontent.put(lang, list_contents);
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
    private String addFulltextPrefix(String target, String fulltext) {
        return "{" + target + "} " + fulltext;
    }

    public boolean isLangSupported(String language) throws SolrServerException, IOException {
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


    /**
     * Return the date and time of the most recent updated metadata, indexed in the field timestamp_update (following the date of update in the metadata collection)
     * @return LocalDateTime, null if no items in the collection
     */
    public ZonedDateTime getLastUpdateMetadata() throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery();
        query.set("q", "*:*");
        query.set("fl", TIMESTAMP_UPDATE_METADATA);
        query.setRows(1);
        query.setSort(TIMESTAMP_UPDATE_METADATA, SolrQuery.ORDER.desc);
        try {
            QueryResponse response = SolrServices.query(fulltextSolr, fulltextCollectionName, query);
            if (response.getResults().size() > 0) {
                return ((Date) response.getResults().get(0).getFieldValue(TIMESTAMP_UPDATE_METADATA)).toInstant()
                        .atZone(ZoneOffset.UTC);

            }
            return null;
        } catch (IOException | SolrServerException e){
            logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Return the date and time of the most recent updated fulltext, indexed in the field timestamp_update_fulltext (following the date of update in the fulltext MongoDB)
     * @return LocalDateTime, null if no items in the collection
     */
    public ZonedDateTime getLastUpdateFulltext() throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery();
        query.set("q", "*:*");
        query.set("fl", TIMESTAMP_UPDATE_FULLTEXT);
        query.setRows(1);
        query.setSort(TIMESTAMP_UPDATE_FULLTEXT, SolrQuery.ORDER.desc);
        try {
            QueryResponse response = SolrServices.query(fulltextSolr, fulltextCollectionName, query);
            if (response.getResults().size() > 0) {
                return ((Date) response.getResults().get(0).getFieldValue(TIMESTAMP_UPDATE_FULLTEXT)).toInstant()
                        .atZone(ZoneOffset.UTC);

            }
            return null;
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
    public Pair<ZonedDateTime, ZonedDateTime> getLastUpdateDates(String europeana_id) throws SolrServerException, IOException {
        try {
            SolrDocument document = SolrServices.get(fulltextSolr, fulltextCollectionName, europeana_id);
            if (document != null) {
                ZonedDateTime mt_ts = ((Date)(document.getFieldValue(TIMESTAMP_UPDATE_METADATA))).toInstant().atZone(ZoneOffset.UTC);
                ZonedDateTime ft_ts = ((Date)(document.getFieldValue(TIMESTAMP_UPDATE_FULLTEXT))).toInstant().atZone(ZoneOffset.UTC);
                return  new Pair(mt_ts,ft_ts);
            }
            return null;
        } catch (SolrServerException | IOException e){
            logger.error(e.getMessage());
            throw e;
        }
    }

    public void commit() throws IOException, SolrServerException {
        SolrServices.commit(fulltextSolr, fulltextCollectionName);
    }

    public static String getEuropeanaId(String dsId, String lcId){ //maybe this logic is implemented already somewhere in the project
        return "/" + dsId +"/" + lcId;
    }

    public static String getDsId(String europeana_id){
        String[] parts = europeana_id.split("/");
        return parts[1];
    }

    public static String getLcId(String europeana_id){
        String[] parts = europeana_id.split("/");
        return parts[2];
    }

    /**
     * Retrieves the document with the id received from the Solr metadata collection. All fields are retrieved
     * @param europeana_id
     * @throws NullPointerException if document does not exist
     * @return
     */
    protected SolrDocument getDocument(String europeana_id) throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery();
        query.set("q", EUROPEANA_ID + ":\"" + europeana_id + "\"");
        query.set("fl", "*"); //retrieve all the fields
        try {
            QueryResponse response = SolrServices.query(fulltextSolr, fulltextCollectionName, query);
            return response.getResults().get(0);
        } catch (IOException | SolrServerException | NullPointerException e){
            logger.error("Error retrieving record " + europeana_id + " - " + e.getMessage());
            throw e;
        }
    }

    public boolean checkMetadata(String europeana_id) throws IOException, SolrServerException {
        SolrDocument mtdoc = metadataCollection.getDocument(europeana_id);
        SolrDocument ftdoc = this.getDocument(europeana_id);
        try {
            for (String f : mtdoc.getFieldNames()) {
                logger.info("processing " + f);
                if (!f.equalsIgnoreCase(VERSION) && !f.equals(TIMESTAMP) && !f.startsWith(FULLTEXT) && !f.equals(ISSUED) && !f.equals(IS_FULLTEXT)) {
                    Collection<Object> ftValues = ftdoc.getFieldValues(f);
                    Collection<Object> mtValues = mtdoc.getFieldValues(f);
                    if (mtValues.size() != ftValues.size() || !mtValues.containsAll(ftValues)
                            || !ftValues.containsAll(mtValues))
                        return false;
                }

            }
            return (Boolean) (ftdoc.getFieldValue(IS_FULLTEXT));
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }

    }

    }

    /**
     * Transforms a SolrDocument into a SolrInputDocument so it can be added to a collection
     * All fields are copied except for _version_ and timestamp, which are automatically added by Solr
     * @param document SolrDocument
     * @return
     */ /*
    protected SolrInputDocument toSolrInputDocument(SolrDocument document) {
        SolrInputDocument inputDocument = new SolrInputDocument();
        for (String field : document.getFieldNames()) {
            if (!field.equals(TIMESTAMP) && !field.equals(VERSION)) { //_version_ and timestamp are automatically added by Solr
                inputDocument.addField(field, document.getFieldValue(field));
            }
        }
        return inputDocument;
    }*/

/*    protected boolean isLangSupported(String language) throws SolrServerException, IOException {
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
    }*/




