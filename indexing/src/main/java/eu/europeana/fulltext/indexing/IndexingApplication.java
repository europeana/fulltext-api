package eu.europeana.fulltext.indexing;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import net.bytebuddy.asm.Advice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
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
    ZonedDateTime lastUpdate = fulltextCollection.getLastUpdateMetadata();
    synchronizeMetadataContent(lastUpdate);
  }
  public void synchronizeMetadataContent(ZonedDateTime lastUpdate) throws IOException, SolrServerException {
    List<String> updated_europeana_ids = metadataCollection.getDocumentsModifiedAfter(lastUpdate);
    synchronizeMetadataContent(updated_europeana_ids);
  }

  public void synchronizeMetadataContent(List<String> europeana_id) throws IOException, SolrServerException {
    fulltextCollection.setMetadata(europeana_id, metadataCollection);
  }

  public void synchronizeFulltextContent() throws IOException, SolrServerException {
    ZonedDateTime lastUpdate = fulltextCollection.getLastUpdateFulltext();
    synchronizeFulltextContent(lastUpdate);
  }

  public void synchronizeFulltextContent(ZonedDateTime lastUpdate) throws IOException, SolrServerException {
    List<AnnoPage> updated = repository.getRecordsModifiedAfter(lastUpdate.toEpochSecond());
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
        ZonedDateTime lastUpdate_ap = ZonedDateTime.from(ap.getModified().toInstant().atZone(ZoneOffset.UTC)); //TODO API: 'modified' in anno page already include time and zone, any way to get them from the Date type?
        String europeana_id = FulltextCollection.getEuropeanaId(ap.getDsId(),ap.getLcId());
        Pair<ZonedDateTime,ZonedDateTime> lastUpdateSolr = fulltextCollection.getLastUpdateDates(europeana_id);
        if (lastUpdateSolr == null) {
          toRepair.add(europeana_id); //the document does not exists in Solr
        } else {
          ZonedDateTime lastUpdate_ftc = lastUpdateSolr.first();
          ZonedDateTime lastUpdate_mtc = lastUpdateSolr.second();
          if (lastUpdate_ftc.isBefore(lastUpdate_ap)) {
          //if (lastUpdate_ftc.isBefore(ChronoZonedDateTime.from(lastUpdate_ap.toInstant().atZone(ZoneOffset.UTC)))) {
            toRepair.add(europeana_id); //fulltext content is not updated
          }
          ZonedDateTime lastUpdate_mt = metadataCollection.getLastUpdateDate(europeana_id);
          if (lastUpdate_mtc.isBefore(lastUpdate_mt)) {
            toRepair.add(europeana_id); //metadata is not updated
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

  private void assertTrue(boolean value) throws Exception {
    if (!value){
      throw new Exception("assertTrue failed");
    }
  }

  private void assertFalse(boolean value) throws Exception {
    if (value){
      throw new Exception("assertFalse failed");
    }
  }

  private void assertEquals(Object obj1, Object obj2) throws Exception {
    if (!obj1.equals(obj2)){
      throw new Exception("assertEquals failed");
    }
  }

  public void fulltextTest() throws Exception {
    List<String> ids = new ArrayList<>();
    ids.add("/9200396/BibliographicResource_3000118435009");
    ids.add("/9200396/BibliographicResource_3000118436165");

    fulltextCollection.deleteDocuments(ids);
    assertFalse(fulltextCollection.exists(ids.get(0)));
    assertFalse(fulltextCollection.exists(ids.get(0)));
    fulltextCollection.setMetadata(ids,metadataCollection);
    assertTrue(fulltextCollection.checkMetadata(ids.get(0)));
    assertTrue(fulltextCollection.checkMetadata(ids.get(1)));
    fulltextCollection.setFulltext(ids);
    assertEquals(ZonedDateTime.of(LocalDateTime.of(2018, Month.JULY,11,14,54,57,295000000),ZoneOffset.UTC),fulltextCollection.getLastUpdateMetadata());
    assertEquals(ZonedDateTime.of(LocalDateTime.of(2018, Month.OCTOBER,23,9,5,35,490000000),ZoneOffset.UTC),fulltextCollection.getLastUpdateFulltext());
    assertTrue(fulltextCollection.exists(ids.get(0)));
    assertTrue(fulltextCollection.exists(ids.get(1)));
    assertEquals(new Pair<LocalDateTime,LocalDateTime>(LocalDateTime.of(2018,Month.JULY,11,14,52,41,794000000),LocalDateTime.of(2018,Month.OCTOBER,23,9,5,35,490000000)),fulltextCollection.getLastUpdateDates(ids.get(0)));
    assertEquals(new Pair<LocalDateTime,LocalDateTime>(LocalDateTime.of(2018,Month.JULY,11,14,54,57,295000000),LocalDateTime.of(2018,Month.OCTOBER,23,9,0,52,508000000)),fulltextCollection.getLastUpdateDates(ids.get(1)));
    fulltextCollection.deleteDocuments(ids);
    fulltextCollection.commit();
  }

  public void metadataTest() throws Exception {
    List<String> ids = new ArrayList<>();
    ids.add("/9200396/BibliographicResource_3000118435009");
    ids.add("/9200396/BibliographicResource_3000118436165");


    assertEquals(LocalDateTime.of(2018,Month.JULY,11,14,52,41,794000000),metadataCollection.getLastUpdateDate(ids.get(0)));
    assertEquals(LocalDateTime.of(2018,Month.JULY,11,14,54,57,295000000),metadataCollection.getLastUpdateDate(ids.get(1)));

    LocalDateTime date = LocalDateTime.of(2022,Month.APRIL,28,15,00,04,0);
    ZonedDateTime dateZone = ZonedDateTime.of(date,ZoneOffset.UTC);

    List<String> modified = metadataCollection.getDocumentsModifiedAfter(dateZone);
  }


  @Override
  public void run(String... args) throws Exception {
    //fulltextTest();
    //metadataTest();
    List<AnnoPage> list = repository.getActive("9200396","BibliographicResource_3000118435009");
  }
}
