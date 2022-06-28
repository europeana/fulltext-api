package eu.europeana.fulltext.indexing;

import static eu.europeana.fulltext.indexing.Constants.FULLTEXT_SOLR_BEAN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.morphia.query.MorphiaCursor;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;
import eu.europeana.fulltext.indexing.utils.AddService;
import eu.europeana.fulltext.indexing.utils.Buffer;
import eu.europeana.fulltext.indexing.utils.DeleteService;
import eu.europeana.fulltext.indexing.utils.SolrServices;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.TupleStream;
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
import java.util.concurrent.TimeUnit;
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

    private Buffer<SolrInputDocument> bufferAdditions;
    private Buffer<String> bufferDeletions;

    @Autowired
    private IndexingAnnoPageRepository repository;

    public FulltextCollection(
        @Qualifier(FULLTEXT_SOLR_BEAN) CloudSolrClient fulltextSolr,
        MetadataCollection metadataCollection) {
        this.fulltextSolr = fulltextSolr;
        this.fulltextCollectionName = fulltextSolr.getDefaultCollection();
        this.metadataCollection = metadataCollection;
        this.bufferAdditions = new Buffer<>(100,new AddService(fulltextSolr,fulltextCollectionName)); //TODO: take buffer capacity from properties
        this.bufferDeletions = new Buffer<>(100, new DeleteService(fulltextSolr,fulltextCollectionName)); //TODO: take buffer capacity from properties
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
     * Updates the metadata of the records contained in this collection. In order to do that, it takes the last update of the metadata
     * (max timestamp_update) and iterates over the records in the Solr metadata collection modified after that date.
     * If there are no records in this (fulltext) collection, nothing is done.
     * @throws IOException
     * @throws SolrServerException
     */
    public void synchronizeMetadataContent() throws IOException, SolrServerException {
        ZonedDateTime lastUpdate = getLastUpdateMetadata();
        if (lastUpdate != null) {
            synchronizeMetadataContent(lastUpdate);
        } //else, no records in collection so no need to update metadata
    }

    /**
     * Updates the metadata of the records contained in this collection. In order to do that, it iterates over the records in the Solr
     * metadata collection modified after the date introduced as parameter (since).
     * If there are no records in this (fulltext) collection, nothing is done.
     * @param since
     * @throws IOException
     * @throws SolrServerException
     */
    public void synchronizeMetadataContent(ZonedDateTime since) throws IOException, SolrServerException {
        List<TupleStream> streams = metadataCollection.getDocumentsModifiedAfter(since);
        for (TupleStream solrStream: streams){
            try {
                solrStream.open();
                Tuple tuple = solrStream.read();
                while (!tuple.EOF) {
                    String europeana_id = tuple.getString(EUROPEANA_ID);
                    if (exists(europeana_id)) {
                        setMetadata(europeana_id, metadataCollection);
                    }
                    tuple = solrStream.read();
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            } finally {
                solrStream.close();
            }
        }
        commit(); //necessary to send records that may be in the buffer, and then commit in Solr (commit is optional)
    }

    /**
     * Updates the fulltext content of the records in this collection. In order to do that, it takes the last update of the fulltext
     * content (max timestamp_update_fulltext) and iterates over the documents in the database modified after that.
     * If a record is in the database but not in this collection, it will be indexed.
     * If a record is in this (fulltext) collection but has been deleted in the database (i.e., all its web resources have been deleted), it
     * will be deleted in this (fulltext) collection.
     * @throws Exception
     */
    public void synchronizeFulltextContent() throws Exception {
        ZonedDateTime lastUpdate = getLastUpdateFulltext();
        if (lastUpdate == null) {
            lastUpdate = ZonedDateTime.ofInstant(Instant.EPOCH,ZoneOffset.UTC);
            logger.info("No records in fulltext collection, proceed to reindex the whole collection");
        }
        synchronizeFulltextContent(lastUpdate);
    }

    /**
     * Updates the fulltext content of the records in this collection. In order to do that, it iterates over the documents in the
     * database modified after the date introduced as parameter (since).
     * If a record is in the database but not in this collection, it will be indexed.
     * If a record is in this (fulltext) collection but has been deleted in the database (i.e., all its web resources have been deleted), it
     * will be deleted in this (fulltext) collection.
     * @param since
     * @throws Exception
     */
    public void synchronizeFulltextContent(ZonedDateTime since) throws Exception {
        Set<String> ids_processed = new HashSet<>();
        MorphiaCursor<AnnoPage> cursorModified = repository.getRecordsModifiedAfter_stream(Date.from(since.toInstant()));
        while (cursorModified.hasNext()) {
            AnnoPage ap = cursorModified.next();
            String europeana_id = FulltextCollection.getEuropeanaId(ap.getDsId(), ap.getLcId());
            if (!ids_processed.contains(europeana_id)) {
                if (!exists(europeana_id)) {
                    setMetadata(europeana_id,metadataCollection);
                    setFulltext(europeana_id);
                } else if (!repository.existsActive(ap.getDsId(), ap.getLcId())) { //TODO: not tested
                    deleteDocument(europeana_id);
                } else {
                    setFulltext(europeana_id);
                }
                ids_processed.add(europeana_id);
            }
        }
        commit(); //necessary to send records that may be in the buffer, and then commit in Solr (commit is optional)
    }

    /**
     * Updates the fulltext content of the records with europeana_id in europeana_ids in this collection.
     * If a record is in the database but not in this collection, it will be indexed.
     * If a record is in this (fulltext) collection but has been deleted in the database (i.e., all its web resources have been deleted), it
     * will be deleted in this (fulltext) collection.
     * @param europeana_ids
     * @throws Exception
     */
    public void synchronizeFulltextContent(List<String> europeana_ids) throws Exception {
        for (String europeana_id: europeana_ids){
            if (!exists(europeana_id)) {
                setMetadata(europeana_id,metadataCollection);
                setFulltext(europeana_id);
            } else if (!repository.existsActive(FulltextCollection.getDsId(europeana_id), FulltextCollection.getLcId(europeana_id))) { //TODO: not tested
                deleteDocument(europeana_id);
            } else {
                setFulltext(europeana_id);
            }
        }
        commit(); //necessary to send records that may be in the buffer, and then commit in Solr (commit is optional)
    }

    /**
     * Retrieves records in this fulltext collection for which the fulltext content is not updated. In order
     * to do that it iterates over all the documents in the database (very intensive process).
     * The metadata contents are not checked.
     * @return
     * @throws IOException
     * @throws SolrServerException
     */
    public List<String> isFulltextUpdated() throws IOException, SolrServerException {
        commit(); //necessary to commit in Solr before checking
        logger.info("Commit done");
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
        logger.info("Start checking");
        Set<String> toRepair = new HashSet<>();
        MorphiaCursor<AnnoPage> cursorActive = repository.getAll();
        List<String> processed = new ArrayList<>();
        while (cursorActive.hasNext()){
            AnnoPage ap = cursorActive.next();
            String europeana_id = FulltextCollection.getEuropeanaId(ap.getDsId(),ap.getLcId());
            if (!processed.contains(europeana_id)) {
                processed.add(europeana_id);
                if (!isFulltextUpdated(europeana_id)){
                    toRepair.add(europeana_id); //document is not in the Solr collection or fulltext content is not updated
                }
            }
        }
        logger.info("End checking: " + toRepair.size() +" documents should be reprocessed");
        return new ArrayList<>(toRepair);
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
                        .atZone(ZoneOffset.UTC); //dates in Solr are always in format ISO8601 and UTC

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
            logger.info("No records in the fulltext collection");
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


    public Pair<ZonedDateTime, ZonedDateTime> getLastUpdateDates(String europeana_id) throws SolrServerException, IOException {
        try {
            SolrDocument document = SolrServices.get(fulltextSolr, fulltextCollectionName, europeana_id);
            if (document != null) {
                ZonedDateTime mt_ts = ((Date)(document.getFieldValue(TIMESTAMP_UPDATE_METADATA))).toInstant().atZone(ZoneOffset.UTC); //dates in Solr are always in format ISO8601 and UTC
                ZonedDateTime ft_ts = ((Date)(document.getFieldValue(TIMESTAMP_UPDATE_FULLTEXT))).toInstant().atZone(ZoneOffset.UTC);
                return  new Pair(mt_ts,ft_ts);
            }
            return null;
        } catch (SolrServerException | IOException e){
            logger.error(e.getMessage());
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


    /**
     * Remove from the Solr fulltext collection all the documents with the ids received
     * @param europeana_id
     * @throws IOException
     * @throws SolrServerException
     */
    protected void deleteDocument(String europeana_id) throws IOException, SolrServerException {
        //TODO: thread
        try {
            bufferDeletions.add(europeana_id);
        } catch (SolrServerException | IOException e){
            logger.error(e.getMessage());
            throw e;
        }

    }

    /**
     * Updates the metadata (atomic updates) in the Solr fulltext collection with that in the Solr metadata collection for the ids received
     * If the document does not exist, it will be created
     * @param europeana_id
     * @throws IOException
     * @throws SolrServerException
     */
    protected void setMetadata(String europeana_id, MetadataCollection metadataCollection) throws IOException, SolrServerException {
        //TODO: thread
        try {
            SolrDocument existingDocument = metadataCollection.getDocument(europeana_id);
            if (existingDocument == null){
                logger.error("Error setting metadata: document "+ europeana_id + " does not exist in metadata collection");
                throw new IllegalArgumentException("Error setting metadata: document "+ europeana_id + " does not exist in metadata collection");
            }
            SolrInputDocument newDocument = new SolrInputDocument();
            newDocument.addField(EUROPEANA_ID, europeana_id);
            for (String field : existingDocument.getFieldNames()) {
                if (field.equals(PROXY_ISSUED)) {
                    Collection<Object> list_issued_dates = existingDocument.getFieldValues(PROXY_ISSUED);
                    List<String> iso_dates = new ArrayList<>();
                    for (Object d : list_issued_dates) {
                        try {
                            LocalDate localDate = LocalDate.parse(d.toString()); //accepts only ISO-8601 extended local date format (https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_LOCAL_DATE)
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
            bufferAdditions.add(newDocument);
        } catch (IOException | SolrServerException e){
            logger.error(e.getMessage());
            throw e;
        }

    }

    /**
     * Updates the fulltext (atomic updates) in the Solr fulltext collection with that in fulltext MongoDB for the ids received
     * If the document does not exist, it will be created
     * @param europeana_id
     * @throws IOException
     * @throws SolrServerException
     */
    protected void setFulltext(String europeana_id) throws IOException, SolrServerException {
        //TODO: thread
        try {
            Map<String, List<String>> lang_ftcontent = new HashMap<>();
            SolrInputDocument newDocument = new SolrInputDocument();
            newDocument.addField(EUROPEANA_ID, europeana_id);
            List<AnnoPage> list_ap = repository.getActive(getDsId(europeana_id), getLcId(europeana_id)); //TODO: is this thread safe?
            if (list_ap.isEmpty()){
                logger.error("Error setting fulltext: document " + europeana_id + " is not in database");
                throw new IllegalArgumentException("Document " + europeana_id + " is not in database");
            }
            Date modified = Date.from(Instant.EPOCH);
            for (AnnoPage ap : list_ap) {
                String fulltext = ap.getRes().getValue();
                String lang = ap.getLang();
                if (!isLangSupported(lang)) {
                    logger.warn("Record " + europeana_id + "-language not supported: " + lang + ". Indexing in fulltext.");
                    lang = "";
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
            for (String lang : lang_ftcontent.keySet()) {
                Map<String, Object> atomicUpdates = new HashMap<>(1);
                atomicUpdates.put("set", lang_ftcontent.get(lang));
                newDocument.addField(FULLTEXT + "." + lang, atomicUpdates);
            }
            Map<String, Object> atomicUpdates = new HashMap<>(1);
            atomicUpdates.put("set", modified);
            newDocument.addField(TIMESTAMP_UPDATE_FULLTEXT, atomicUpdates);
            bufferAdditions.add(newDocument);
        } catch (SolrServerException | IOException e){
            logger.error(e.getMessage());
            throw e;
        }
    }

    protected boolean isFulltextUpdated(String europeana_id) throws IOException, SolrServerException {
        //TODO: thread
        try {
            String dsId = FulltextCollection.getDsId(europeana_id);
            String lcId = FulltextCollection.getLcId(europeana_id);
            List<AnnoPage> ap_list = repository.getActive(dsId,lcId);
            if (ap_list.isEmpty()){
                if (this.exists(europeana_id)){
                    logger.info(europeana_id + " should have been deleted in Solr fulltext collection");
                    return false;
                }
            }
            Date max_modified = ap_list.stream().map(p->p.getModified()).max(Date::compareTo).orElseThrow();
            ZonedDateTime lastUpdate_ap = ZonedDateTime.from(max_modified.toInstant().atZone(ZoneOffset.UTC));
            ZonedDateTime lastUpdate_ft = this.getLastUpdateDates(europeana_id).second();
            if (lastUpdate_ft == null) {
                logger.info(europeana_id + " not found in the Solr fulltext collection");
                return false;

            }
            if (lastUpdate_ft.isBefore(lastUpdate_ap)) {
                logger.info(europeana_id + " fulltext content is not updated");
                return false;
            }
            return true;
        } catch (SolrServerException | IOException e){
            logger.error(e.getMessage());
            throw e;
        }
    }

    protected void commit() throws SolrServerException, IOException {
        try {
            bufferAdditions.dispose();
            bufferDeletions.dispose();
            SolrServices.commit(fulltextSolr, fulltextCollectionName); //TODO: if synchr process is launched too frequently, comment this line
            logger.info("Commit done");
        } catch (SolrServerException | IOException e){
            logger.error(e.getMessage());
            throw e;
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


    /**
     * Retrieves the document with the id received from the Solr metadata collection. All fields are retrieved
     * @param europeana_id
     * @throws NullPointerException if document does not exist
     * @return
     */
    private SolrDocument getDocument(String europeana_id) throws IOException, SolrServerException {
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


}




