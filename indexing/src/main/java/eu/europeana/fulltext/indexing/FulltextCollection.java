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
import eu.europeana.fulltext.indexing.service.AddService;
import eu.europeana.fulltext.indexing.batch.Buffer;
import eu.europeana.fulltext.indexing.service.DeleteService;
import eu.europeana.fulltext.indexing.service.SolrServices;
import eu.europeana.fulltext.util.GeneralUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.SolrStream;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.schema.SchemaRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.params.ModifiableSolrParams;
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
    private static final Logger LOG = LogManager.getLogger(FulltextCollection.class);

    private final CloudSolrClient fulltextSolr;
    private final MetadataCollection metadataCollection;
    private final String fulltextCollectionName;
    private Buffer<SolrInputDocument> bufferAdditions;
    private Buffer<String> bufferDeletions;
    //PUT coreURLs heres
    private final List<String> coreURLs;


    @Autowired
    private IndexingAnnoPageRepository repository;

    public FulltextCollection(
        @Qualifier(FULLTEXT_SOLR_BEAN) CloudSolrClient fulltextSolr,
        MetadataCollection metadataCollection) throws IOException {
        this.fulltextSolr = fulltextSolr;
        this.fulltextCollectionName = fulltextSolr.getDefaultCollection();
        this.metadataCollection = metadataCollection;
        this.bufferAdditions = new Buffer<>(100,new AddService(fulltextSolr,fulltextCollectionName)); //TODO: take buffer capacity from properties
        this.bufferDeletions = new Buffer<>(100, new DeleteService(fulltextSolr,fulltextCollectionName)); //TODO: take buffer capacity from properties
        this.coreURLs = getCoreURLs();

    }

    /**
     * Updates the metadata of the records contained in Fulltext collection.
     * It takes the last update of the metadata (max timestamp_update) and
     * iterates over the records in the Solr metadata collection modified after that date.
     *
     * If there are no records in this (fulltext) collection, do nothing.
     * @throws IOException
     * @throws SolrServerException
     */
 /*   public void synchronizeMetadataContent() throws IOException, SolrServerException {
        ZonedDateTime lastUpdate = getLastUpdateMetadata();
        if (lastUpdate != null) {
            LOG.info("Updating the fulltext collection from {}", lastUpdate);
            synchronizeMetadataContent(lastUpdate);
        }
    }*/

    /**
     * Updates the metadata of the records contained in Fulltext collection.
     *
     * It iterates over the records in the Solr metadata collection modified after the date.
     * If there are no records in fulltext collection, nothing is done.
     * @param since modified date.
     * @throws IOException
     * @throws SolrServerException
     */
/*    public void synchronizeMetadataContent(ZonedDateTime since) throws IOException, SolrServerException {
        int count=0;
        List<TupleStream> streams = metadataCollection.getDocumentsModifiedAfter(since);
        for (TupleStream solrStream: streams) {
            try {
                solrStream.open();
                Tuple tuple = solrStream.read();
                int total=0;
                while (!tuple.EOF) {
                    String europeanaId = tuple.getString(Constants.EUROPEANA_ID);
                    LOG.info(++total + " " + europeanaId);
                    try {
                        // check if the data exists with this europeana Id in Fulltext Solr collection
                        if (existsByEuropeanaID(europeanaId)) {
                            setMetadata(europeanaId, metadataCollection);
                            LOG.info(++count + " : " + europeanaId + " metadata updated");
                        }
                        tuple = solrStream.read();
                    } catch (IOException | SolrServerException e) {
                        LOG.error(" Error synchronising metadata content for " + europeanaId, e.getMessage());
                    }
                }
            } catch (IOException  e){
                LOG.error(e.getMessage());
                solrStream.close();
                commit();
                throw e;
            }
        }
        commit(); //necessary to send records that may be in the buffer, and then commit in Solr (commit in Solr is optional)
    }*/

    /**
     * Updates the metadata of the records contained in Fulltext collection.
     * It iterates over all the records contained in the Solr fulltext collection,
     * and updates the metadata with the corresponding contents in the Solr metadata collection
     * (even when it has not been updated as it seems to be faster this way).
     * If there are no records in the fulltext collection, nothing is done.
     * @throws IOException
     * @throws SolrServerException
     */
    public void synchronizeMetadataContent() throws IOException, SolrServerException {
        int count=0;
        List<TupleStream> streams = getAllDocuments();
        for (TupleStream solrStream: streams) {
            try {
                solrStream.open();
                Tuple tuple = solrStream.read();
                while (!tuple.EOF) {
                    String europeanaId = tuple.getString(Constants.EUROPEANA_ID);
                    try {
                        setMetadata(europeanaId, metadataCollection);
                        LOG.info(++count + " : " + europeanaId + " metadata updated");
                        tuple = solrStream.read();
                    } catch (IOException | SolrServerException e) {
                        LOG.error(" Error synchronising metadata content for " + europeanaId, e.getMessage());
                    }
                }
            } catch (IOException  e){
                LOG.error(e.getMessage());
                solrStream.close();
                commit();
                throw e;
            }
        }
        commit(); //necessary to send records that may be in the buffer, and then commit in Solr (commit in Solr is optional)
    }


    /**
     * Updates the fulltext content of the records in this collection.
     * It takes the last update of the fulltext content (max timestamp_update_fulltext) and
     * iterates over the documents in the database modified after that.
     *
     * If a record is in the database but not in this collection, it will be indexed.
     * If a record is in this (fulltext) collection but has been deleted in the database (i.e., all its web resources have been deleted), it
     * will be deleted in this (fulltext) collection.
     * @throws Exception
     */
    public void synchronizeFulltextContent() throws Exception {
        ZonedDateTime lastUpdate = getLastUpdateFulltext();
        if (lastUpdate == null) {
            lastUpdate = ZonedDateTime.ofInstant(Instant.EPOCH,ZoneOffset.UTC);
            LOG.info("No records in fulltext collection, proceed to reindex the whole collection");
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
        int count = 0;
        Set<String> idsProcessed = new HashSet<>();
        try {
            MorphiaCursor<AnnoPage> cursorModified = repository.getRecordsModifiedAfterStream(Date.from(since.toInstant())); //only date is not valid, it has to include time. E.g., 2022-08-05T09:49:34.141+00:00 instead of 2022-08-05
            SchemaRepresentation schema = getSchema();
            while (cursorModified.hasNext()) {
                AnnoPage ap = cursorModified.next();
                String europeanaId = GeneralUtils.generateRecordId(ap.getDsId(), ap.getLcId());
                try {
                    if (!idsProcessed.contains(europeanaId)) {
                        boolean active = repository.existsActive(ap.getDsId(), ap.getLcId());
                        boolean exists = existsByEuropeanaID(europeanaId);
                        if (active && !exists) {        //add
                            setMetadata(europeanaId, metadataCollection);
                            setFulltext(europeanaId, schema);
                        } else if (!active && exists) { //delete
                            deleteDocument(europeanaId);
                        } else if (active && exists) {   //update
                            setFulltext(europeanaId, schema);
                        }
                        idsProcessed.add(europeanaId);
                        LOG.info(++count + ":" + europeanaId + " fulltext processed");
                    }
                } catch (Exception e) {
                    LOG.error(" Error synchronising fulltext content for " + europeanaId, e.getMessage());
                }
            }
        }catch (Exception e){
            LOG.error(e.getMessage());
            commit();
            throw e;
        }
        commit(); //necessary to send records that may be in the buffer, and then commit in Solr (commit is optional)
    }

    /**
     * Updates the fulltext content of the records with europeana_id in europeana_ids in this collection.
     * If a record is in the database but not in this collection, it will be indexed.
     * If a record is in this (fulltext) collection but has been deleted in the database (i.e., all its web resources have been deleted), it
     * will be deleted in this (fulltext) collection.
     * @param europeanaIds
     * @throws Exception
     */
    public void synchronizeFulltextContent(List<String> europeanaIds) throws Exception {
        try {
            SchemaRepresentation schema = getSchema();
            for (String europeanaId : europeanaIds) {
                try {
                    boolean active = repository.existsActive(GeneralUtils.getDsId(europeanaId), GeneralUtils.getLocalId(europeanaId)); //TODO: not tested
                    boolean exists = existsByEuropeanaID(europeanaId);
                    if (active && !exists) {        //add
                        setMetadata(europeanaId, metadataCollection);
                        setFulltext(europeanaId, schema);
                    } else if (!active && exists) { //delete
                        deleteDocument(europeanaId);
                    } else if (active && exists) {   //update
                        setFulltext(europeanaId, schema);
                    }
                } catch (Exception e) {
                    LOG.error(" Error synchronising fulltext content for  " + europeanaId, e.getMessage());
                }
            }
        }catch (Exception e){
            LOG.error(e.getMessage());
            commit();
            throw e;
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
        LOG.info("Commit done");
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
        LOG.info("Start checking");
        Set<String> toRepair = new HashSet<>();
        MorphiaCursor<AnnoPage> cursorActive = repository.getAll();
        List<String> processed = new ArrayList<>();
        while (cursorActive.hasNext()){
            AnnoPage ap = cursorActive.next();
            String europeanaId = GeneralUtils.generateRecordId(ap.getDsId(),ap.getLcId());
            if (!processed.contains(europeanaId)) {
                processed.add(europeanaId);
                LOG.info("Processing " +  europeanaId);
                if (!isFulltextUpdated(europeanaId)){
                    toRepair.add(europeanaId); //document is not in the Solr collection or fulltext content is not updated
                }
            }
        }
        LOG.info("End checking: {} documents should be reprocessed", toRepair.size());
        return new ArrayList<>(toRepair);
    }


    public boolean isLangSupported(String language, SchemaRepresentation schema) throws SolrServerException, IOException {
        if (language == null || language.isEmpty())
            return false;
        if (!schema.getFields().stream().map(p -> p.get("name")).collect(Collectors.toList())
                .contains(Constants.FULLTEXT + "." + language)) {
            return false;
        }
        return true;
    }

    public SchemaRepresentation getSchema() throws SolrServerException, IOException {
        SchemaRequest request = new SchemaRequest();
        SchemaResponse response = request.process(fulltextSolr, fulltextCollectionName);
        SchemaRepresentation schema = response.getSchemaRepresentation();
        return schema;
    }


    /**
     * Returns the date and time of the most recent updated metadata,
     * indexed in the field timestamp_update (following the date of update in the metadata collection)
     * @return LocalDateTime, null if no items in the collection
     */
    public ZonedDateTime getLastUpdateMetadata() throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery();
        query.set(Constants.SOLR_QUERY, Constants.SOLR_QUERY_DEFAULT);
        query.set(Constants.SOLR_FL, Constants.TIMESTAMP_UPDATE_METADATA);
        query.setRows(1);
        query.setSort(Constants.TIMESTAMP_UPDATE_METADATA, SolrQuery.ORDER.desc);
        try {
            QueryResponse response = SolrServices.query(fulltextSolr, fulltextCollectionName, query);
            if (response.getResults().size() > 0) {
                return ((Date) response.getResults().get(0).getFieldValue(Constants.TIMESTAMP_UPDATE_METADATA)).toInstant()
                        .atZone(ZoneOffset.UTC); //dates in Solr are always in format ISO8601 and UTC
            }
        } catch (IOException | SolrServerException e){
            LOG.error("Error getting date and time of the most recent updated metadata.", e.getMessage());
            throw e;
        }
        return null;
    }

    /**
     * Return the date and time of the most recent updated fulltext, indexed in the field timestamp_update_fulltext
     * (following the date of update in the fulltext MongoDB)
     * @return LocalDateTime, null if no items in the collection
     */
    public ZonedDateTime getLastUpdateFulltext() throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery();
        query.set(Constants.SOLR_QUERY, Constants.SOLR_QUERY_DEFAULT);
        query.set(Constants.SOLR_FL, Constants.TIMESTAMP_UPDATE_FULLTEXT);
        query.setRows(1);
        query.setSort(Constants.TIMESTAMP_UPDATE_FULLTEXT, SolrQuery.ORDER.desc);
        try {
            QueryResponse response = SolrServices.query(fulltextSolr, fulltextCollectionName, query);
            if (response.getResults().size() > 0) {
                return ((Date) response.getResults().get(0).getFieldValue(Constants.TIMESTAMP_UPDATE_FULLTEXT)).toInstant()
                        .atZone(ZoneOffset.UTC);

            }
            LOG.info("No records in the fulltext collection");
            return null;
        } catch (IOException | SolrServerException e){
            LOG.error("Error fetching date and time of the most recent updated fulltext.", e.getMessage());
            throw e;
        }
    }

    /**
     * Check if the document with europeana_id exists in Fulltext Solr collection
     * @param europeanaId
     * @return
     * @throws SolrServerException
     * @throws IOException
     */
    public boolean existsByEuropeanaID(String europeanaId) throws SolrServerException, IOException {
        try {
            return (SolrServices.get(fulltextSolr, fulltextCollectionName, europeanaId) != null) ;
        } catch (SolrServerException | IOException e){
            LOG.error("Error fetching the document from Fulltext Solr for europeana id {} ", europeanaId, e.getMessage());
            throw e;
        }
    }


    public Pair<ZonedDateTime, ZonedDateTime> getLastUpdateDates(String europeanaId) throws SolrServerException, IOException {
        try {
            SolrDocument document = SolrServices.get(fulltextSolr, fulltextCollectionName, europeanaId);
            if (document != null) {
                ZonedDateTime mtTs = ((Date)(document.getFieldValue(Constants.TIMESTAMP_UPDATE_METADATA))).toInstant().atZone(ZoneOffset.UTC); //dates in Solr are always in format ISO8601 and UTC
                ZonedDateTime ftTs = ((Date)(document.getFieldValue(Constants.TIMESTAMP_UPDATE_FULLTEXT))).toInstant().atZone(ZoneOffset.UTC);
                return  new Pair(mtTs,ftTs);
            }
            return null;
        } catch (SolrServerException | IOException e){
            LOG.error(e.getMessage());
            throw e;
        }
    }


    public boolean checkMetadata(String europeanaId) throws IOException, SolrServerException {
        SolrDocument mtdoc = metadataCollection.getDocument(europeanaId);
        SolrDocument ftdoc = this.getDocument(europeanaId);
        try {
            for (String f : mtdoc.getFieldNames()) {
                LOG.info("processing {}", f);
                if (!f.equalsIgnoreCase(Constants.VERSION) && !f.equals(Constants.TIMESTAMP) && !f.startsWith(Constants.FULLTEXT) && !f.equals(Constants.ISSUED)
                        && !f.equals(Constants.IS_FULLTEXT)) {
                    Collection<Object> ftValues = ftdoc.getFieldValues(f);
                    Collection<Object> mtValues = mtdoc.getFieldValues(f);
                    if (mtValues.size() != ftValues.size() || !mtValues.containsAll(ftValues)
                            || !ftValues.containsAll(mtValues))
                        return false;
                }

            }
            return (Boolean) (ftdoc.getFieldValue(Constants.IS_FULLTEXT));
        } catch (Exception e) {
            LOG.error(e.getMessage());
            throw e;
        }

    }


    /**
     * Remove from the Solr fulltext collection all the documents with the ids received
     * @param europeanaId
     * @throws IOException
     * @throws SolrServerException
     */
    protected void deleteDocument(String europeanaId) throws IOException, SolrServerException {
        //TODO: thread
        try {
            bufferDeletions.add(europeanaId);
        } catch (SolrServerException | IOException e){
            LOG.error(e.getMessage());
            throw e;
        }

    }

    /**
     * Updates the metadata (atomic updates) in the Solr fulltext collection with that in the Solr metadata collection for the ids received
     * If the document does not exist, it will be created
     * @param europeanaId
     * @throws IOException
     * @throws SolrServerException
     */
    protected void setMetadata(String europeanaId, MetadataCollection metadataCollection) throws IOException, SolrServerException {
        //TODO: thread
        try {
            SolrDocument existingDocument = metadataCollection.getDocument(europeanaId);
            if (existingDocument == null){
                LOG.error("Error setting metadata. Document with europeana_id {} does not exist in metadata collection", europeanaId);
                throw new IllegalArgumentException("Error setting metadata: document "+ europeanaId + " does not exist in metadata collection");
            }
            SolrInputDocument newDocument = new SolrInputDocument();
            newDocument.addField(Constants.EUROPEANA_ID, europeanaId);
            for (String field : existingDocument.getFieldNames()) {
                if (field.equals(Constants.PROXY_ISSUED)) {
                    Collection<Object> listIssuedDates = existingDocument.getFieldValues(Constants.PROXY_ISSUED);
                    List<String> isoDates = new ArrayList<>();
                    for (Object d : listIssuedDates) {
                        try {
                            //accepts only ISO-8601 extended local date format (https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_LOCAL_DATE)
                            LocalDate localDate = LocalDate.parse(d.toString());
                            ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.of("Z"));
                            isoDates.add(zonedDateTime.format(DateTimeFormatter.ISO_INSTANT));
                        } catch (DateTimeException e) {
                            LOG.warn("Not parsable date in record  {} : {}", europeanaId, d.toString());
                        }
                        if (!isoDates.isEmpty()) {
                            Map<String, Object> atomicUpdates = new HashMap<>(1);
                            atomicUpdates.put("set", isoDates);
                            newDocument.addField(Constants.ISSUED, atomicUpdates);
                        }
                    }
                }
                if (field.equals(Constants.IS_FULLTEXT)){
                    Map<String, Object> atomicUpdates = new HashMap<>(1);
                    atomicUpdates.put("set", true);
                    newDocument.addField(field, atomicUpdates);
                }
                if (!field.equals(Constants.EUROPEANA_ID) && !field.equals(Constants.TIMESTAMP) &&
                        !field.equals(Constants.VERSION) && !field.equals(Constants.IS_FULLTEXT)) {
                    //_version_ and timestamp are automatically added by Solr
                    Map<String, Object> atomicUpdates = new HashMap<>(1);
                    atomicUpdates.put("set", existingDocument.getFieldValue(field));
                    newDocument.addField(field, atomicUpdates);
                }
            }
            bufferAdditions.add(newDocument);
        } catch (IOException | SolrServerException e){
            LOG.error("Error setting the metadata ", e.getMessage());
            throw e;
        }

    }

    /**
     * Updates the fulltext (atomic updates) in the Solr fulltext collection with that in fulltext MongoDB for the ids received
     * If the document does not exist, it will be created
     * @param europeanaId
     * @throws IOException
     * @throws SolrServerException
     */
    protected void setFulltext(String europeanaId, SchemaRepresentation schema) throws IOException, SolrServerException {
        //TODO: thread
        try {
            Map<String, List<String>> langFtContent = new HashMap<>();
            SolrInputDocument newDocument = new SolrInputDocument();
            newDocument.addField(Constants.EUROPEANA_ID, europeanaId);
            //List<AnnoPage> listAp = repository.findActiveAnnoPage(GeneralUtils.getDsId(europeanaId), GeneralUtils.getLocalId(europeanaId));

            //best performance if this is possible
            //List<AnnoPage> listAp = repository.findAnnoPage(GeneralUtils.getDsId(europeanaId), GeneralUtils.getLocalId(europeanaId)); //TODO: is this thread safe?
            //List<AnnoPage> active = listAp.stream().filter(p -> p.getDeleted() != null).collect(Collectors.toList());
            //List<AnnoPage> deleted = listAp.stream().filter(p -> p.getDeleted() == null).collect(Collectors.toList());

            List<AnnoPage> active = repository.findActiveAnnoPage(GeneralUtils.getDsId(europeanaId), GeneralUtils.getLocalId(europeanaId)); //TODO: maybe better performance with only one query
            List<AnnoPage> deleted = repository.findDeletedAnnoPage(GeneralUtils.getDsId(europeanaId), GeneralUtils.getLocalId(europeanaId));
            if (active.isEmpty()){ //we have to have at least one active in this point of the code, but just in case
                LOG.error("Error setting fulltext. Document with europeana_id {} is not in database", europeanaId);
                throw new IllegalArgumentException("Document " + europeanaId + " is not in database");
            }
            Date modified = Date.from(Instant.EPOCH);
            for (AnnoPage ap : active) {
                String fulltext = ap.getRes().getValue();
                String lang = ap.getLang();
                if (!isLangSupported(lang, schema)) {
                    LOG.warn("Record {} - language not supported: {} . Indexing in fulltext.", europeanaId, lang);
                    lang = "";
                }
                String target = ap.getTgtId();
                Date apModified = ap.getModified();
                if (modified.before(apModified)) {
                    modified = apModified;
                }
                String content = addFulltextPrefix(target, fulltext);
                List<String> listContents = langFtContent.get(lang);
                if (listContents == null) {
                    listContents = new ArrayList<>();
                    langFtContent.put(lang, listContents);
                }
                listContents.add(content);

            }
            for (AnnoPage ap: deleted){
                String lang = ap.getLang();
                if (!isLangSupported(lang, schema)) {
                    LOG.warn("Record {} - language not supported: {} . Indexing in fulltext.", europeanaId, lang);
                    lang = "";
                }
                if (!langFtContent.containsKey(lang)){
                    langFtContent.put(lang, new ArrayList<>()); //hopefully removes content (although not the field)
                }
            }
            for (String lang : langFtContent.keySet()) {
                Map<String, Object> atomicUpdates = new HashMap<>(1);
                atomicUpdates.put("set", langFtContent.get(lang));
                newDocument.addField(Constants.FULLTEXT + "." + lang, atomicUpdates);
            }
            Map<String, Object> atomicUpdates = new HashMap<>(1);
            atomicUpdates.put("set", modified);
            newDocument.addField(Constants.TIMESTAMP_UPDATE_FULLTEXT, atomicUpdates);
            bufferAdditions.add(newDocument);
        } catch (SolrServerException | IOException e){
            LOG.error(e.getMessage());
            throw e;
        }
    }

    protected boolean isFulltextUpdated(String europeanaId) throws IOException, SolrServerException {
        //TODO: thread
        try {
            String dsId = GeneralUtils.getDsId(europeanaId);
            String lcId = GeneralUtils.getLocalId(europeanaId);
            List<AnnoPage> ap_list = repository.findActiveAnnoPage(dsId,lcId);
            if (ap_list.isEmpty()){
                if (this.existsByEuropeanaID(europeanaId)){
                    LOG.info("{} should have been deleted in Solr fulltext collection.", europeanaId);
                    return false;
                }
                return true;
            }
            Date max_modified = ap_list.stream().map(p->p.getModified()).max(Date::compareTo).orElseThrow();
            ZonedDateTime lastUpdate_ap = ZonedDateTime.from(max_modified.toInstant().atZone(ZoneOffset.UTC));
            ZonedDateTime lastUpdate_ft = this.getLastUpdateDates(europeanaId).second();
            if (lastUpdate_ft == null) {
                LOG.info(" {} not found in the Solr fulltext collection", europeanaId);
                return false;
            }
            if (lastUpdate_ft.isBefore(lastUpdate_ap)) {
                LOG.info(" {} fulltext content is not updated", europeanaId);
                return false;
            }
            return true;
        } catch (SolrServerException | IOException e){
            LOG.error(e.getMessage());
            throw e;
        }
    }

    public List<TupleStream> getAllDocuments() {
        try {
            List<TupleStream> streams = new ArrayList<>();
            for (String coreURL : coreURLs) { //we have to iterate each core separately
                ModifiableSolrParams params = new ModifiableSolrParams();
                params.set(Constants.SOLR_QUERY, Constants.SOLR_QUERY_DEFAULT);
                params.set(Constants.SOLR_QT, Constants.SOLR_EXPORT);
                params.set(Constants.SOLR_SORT, Constants.EUROPEANA_ID + Constants.SOLR_SORT_ASC);
                params.set(Constants.SOLR_FL, Constants.EUROPEANA_ID);

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

    protected void commit() throws SolrServerException, IOException {
        try {
            bufferAdditions.dispose();
            bufferDeletions.dispose();
            SolrServices.commit(fulltextSolr, fulltextCollectionName); //TODO: if synchr process is launched too frequently, comment this line
            LOG.info("Commit done");
        } catch (SolrServerException | IOException e){
            LOG.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Prefix added to each fulltext content. It will be stored but not indexed.
     * It is useful for the API to locate the matched terms and highlight them in the image (if applicable)
     * @param fulltext
     * @param target
     * @return
     */
    private String addFulltextPrefix(String target, String fulltext) {
        return "{" + target + "} " + fulltext;
    }

    /**
     * Retrieves the document with the id received from the Solr metadata collection. All fields are retrieved
     * @param europeanaId
     * @throws NullPointerException if document does not exist
     * @return
     */
    private SolrDocument getDocument(String europeanaId) throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery();
        query.set(Constants.SOLR_QUERY, Constants.EUROPEANA_ID + ":\"" + europeanaId + "\"");
        query.set(Constants.SOLR_FL, Constants.ALL); //retrieve all the fields
        try {
            QueryResponse response = SolrServices.query(fulltextSolr, fulltextCollectionName, query);
            return response.getResults().get(0);
        } catch (IOException | SolrServerException | NullPointerException e){
            LOG.error("Error retrieving record {}. {}", europeanaId , e.getMessage());
            throw e;
        }
    }

    private List<String> getCoreURLs() throws IOException {
        try {
            List<String> coreList = new ArrayList<String>();
            for (Replica replica : fulltextSolr.getClusterStateProvider().getClusterState().getCollection(fulltextCollectionName).getReplicas()) {
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




