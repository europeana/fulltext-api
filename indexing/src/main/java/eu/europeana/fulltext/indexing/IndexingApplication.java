package eu.europeana.fulltext.indexing;

import dev.morphia.query.Meta;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.indexing.config.DataSourceConfig;
import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IndexingApplication implements CommandLineRunner {

  @Autowired
  private IndexingAnnoPageRepository repository;

  @Autowired
  private MetadataCollection metadataCollection;

  @Autowired
  private FulltextCollection fulltextCollection;

  private static final Logger logger = LogManager.getLogger(IndexingApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(IndexingApplication.class, args);
  }

  public void synchronizeMetadataContent() throws IOException, SolrServerException {
    LocalDateTime lastUpdate = fulltextCollection.getLastUpdateMetadata();
    synchronizeMetadataContent(lastUpdate);
  }
  public void synchronizeMetadataContent(LocalDateTime lastUpdate) throws IOException, SolrServerException {
    List<String> updated_europeana_ids = metadataCollection.getDocumentsModifiedAfter(lastUpdate);
    synchronizeMetadataContent(updated_europeana_ids);
  }

  public void synchronizeMetadataContent(List<String> europeana_id) throws IOException, SolrServerException {
    fulltextCollection.setMetadata(europeana_id, metadataCollection);
  }

  public void synchronizeFulltextContent() throws IOException, SolrServerException {
    LocalDateTime lastUpdate = fulltextCollection.getLastUpdateFulltext();
    synchronizeFulltextContent(lastUpdate);
  }

  public void synchronizeFulltextContent(LocalDateTime lastUpdate) throws IOException, SolrServerException {
    List<AnnoPage> updated = repository.getRecordsModifiedAfter(lastUpdate.toEpochSecond(ZoneOffset.UTC));
    List<String> updated_europeana_ids = updated.stream().map(p-> FulltextCollection.getEuropeanaId(p.getDsId(),p.getLcId())).distinct().collect(Collectors.toList());
    synchronizeFulltextContent(updated_europeana_ids);
  }

  public void synchronizeFulltextContent(List<String> europeana_id) throws IOException, SolrServerException {
    List<String> toAdd = new ArrayList<>();
    List<String> toDelete = new ArrayList<>();
    List<String> toUpdate = new ArrayList<>();
    for (String id: europeana_id){
      if (!fulltextCollection.exists(id)){
        toAdd.add(id);
      } else if (repository.getActive(FulltextCollection.getDsId(id), FulltextCollection.getLcId(id)).isEmpty()){
        toDelete.add(id);
      } else {
        toUpdate.add(id);
      }
    }
    if (!toAdd.isEmpty()) {
      //fulltextCollection.addDocuments(toAdd,metadataCollection); not necessary
      fulltextCollection.setMetadata(toAdd,metadataCollection);
      fulltextCollection.setFulltext(toAdd);
    }
    if (!toDelete.isEmpty()){
      fulltextCollection.deleteDocuments(toDelete);
    }
    if (!toUpdate.isEmpty()){
      fulltextCollection.setFulltext(toUpdate);
    }
  }


  public List<String> check() throws IOException, SolrServerException {
    Set<String> toRepair = new HashSet<>();
    Iterator<AnnoPage> iteratorActive = repository.getActive();
      while (iteratorActive.hasNext()){
        AnnoPage ap = iteratorActive.next();
        Date lastUpdate_ap = ap.getModified();
        String europeana_id = FulltextCollection.getEuropeanaId(ap.getDsId(),ap.getLcId());
        Pair<LocalDateTime,LocalDateTime> lastUpdateSolr = fulltextCollection.getLastUpdateDates(europeana_id);
        if (lastUpdateSolr == null) {
          toRepair.add(europeana_id); //the document does not exists in Solr
        } else {
          LocalDateTime lastUpdate_ftc = lastUpdateSolr.first();
          LocalDateTime lastUpdate_mtc = lastUpdateSolr.second();
          if (lastUpdate_ftc.isBefore(ChronoLocalDateTime.from(lastUpdate_ap.toInstant().atZone(ZoneOffset.UTC)))) {
            toRepair.add(europeana_id); //fulltext content is not updated
          } else {
            LocalDateTime lastUpdate_mt = metadataCollection.getLastUpdateDate(europeana_id);
            if (lastUpdate_mtc.isBefore(ChronoLocalDateTime.from(lastUpdate_mt))) {
              toRepair.add(europeana_id); //metadata is not updated
            }
          }
        }
      }
      Iterator<AnnoPage> iteratorDeleted = repository.getDeleted();
      while (iteratorDeleted.hasNext()) {
        AnnoPage ap = iteratorDeleted.next();
        if (repository.getActive(ap.getDsId(),ap.getLcId()).isEmpty()) {
          String europeana_id = FulltextCollection.getEuropeanaId(ap.getDsId(), ap.getLcId());
          if (fulltextCollection.exists(europeana_id)) {
            toRepair.add(europeana_id); //the document has not been deleted in Solr
          }
        }
      }
      logger.warn("Documents to be reprocessed to update fulltext content: " + toRepair.size());
      return new ArrayList<>(toRepair);
  }



  @Override
  public void run(String... args) throws Exception {
/*    Properties p = new Properties();
    p.load(IndexingApplication.class.getClassLoader().getResourceAsStream("indexing.user.properties"));
    metadataCollection = new MetadataCollection(p.getProperty("solr.metadata.url").split(","),p.getProperty("solr.metadata.collection"));
    fulltextCollection = new FulltextCollection(p.getProperty("solr.fulltext.url").split(","),p.getProperty("solr.fulltext.collection"));

 */
    //List<AnnoPage> annoPages = repository.getRecordsModifiedAfter(1654034400000L);

    List<String> ids = new ArrayList<>();
    ids.add("/9200396/BibliographicResource_3000118435009");
    ids.add("/9200396/BibliographicResource_3000118436165");
    String[] url = {"http://solr-2-rnd.eanadev.org:9191/solr"};
    //CloudSolrClient client = new CloudSolrClient.Builder(Arrays.asList(url)).build();
    //client.setDefaultCollection("fulltext");


   // fulltextCollection.deleteDocuments(ids);
   // boolean exists1 = fulltextCollection.exists(ids.get(0));
   // fulltextCollection.commit();
    boolean exists2 = fulltextCollection.exists(ids.get(0));
 //   fulltextCollection.addDocuments(ids,metadataCollection);
  //  fulltextCollection.commit();
    fulltextCollection.setMetadata(ids,metadataCollection);
    fulltextCollection.commit();
    boolean valid1 = fulltextCollection.checkMetadata(ids.get(0));
    boolean valid2 = fulltextCollection.checkMetadata(ids.get(1));
    fulltextCollection.setFulltext(ids);
    fulltextCollection.commit();
    LocalDateTime mtdt = fulltextCollection.getLastUpdateMetadata();
    LocalDateTime ftdt = fulltextCollection.getLastUpdateFulltext();
    boolean exists = fulltextCollection.exists(ids.get(0));
    Pair<LocalDateTime,LocalDateTime> dates = fulltextCollection.getLastUpdateDates(ids.get(0));
    List<AnnoPage> annoPages = repository.getActive("9200396","BibliographicResource_3000118436165");
    logger.info("{}", annoPages);

  }
}
