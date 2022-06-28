package eu.europeana.fulltext.indexing;

import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;
import java.io.IOException;
import java.time.*;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.io.stream.TupleStream;
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


  //TEST
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
    fulltextCollection.deleteDocument(ids.get(0));
    fulltextCollection.deleteDocument(ids.get(1));
    fulltextCollection.commit();
    assertFalse(fulltextCollection.exists(ids.get(0)));
    assertFalse(fulltextCollection.exists(ids.get(1)));
    fulltextCollection.setMetadata(ids.get(0),metadataCollection);
    fulltextCollection.setMetadata(ids.get(1),metadataCollection);
    fulltextCollection.commit();
    assertTrue(fulltextCollection.checkMetadata(ids.get(0)));
    assertTrue(fulltextCollection.checkMetadata(ids.get(1)));
    fulltextCollection.setFulltext(ids.get(0));
    fulltextCollection.setFulltext(ids.get(1));
    fulltextCollection.commit();
    assertEquals(ZonedDateTime.of(LocalDateTime.of(2018, Month.JULY,11,14,54,57,295000000), ZoneOffset.UTC),fulltextCollection.getLastUpdateMetadata());
    assertEquals(ZonedDateTime.of(LocalDateTime.of(2018, Month.OCTOBER,23,9,5,35,490000000),ZoneOffset.UTC),fulltextCollection.getLastUpdateFulltext());
    assertTrue(fulltextCollection.exists(ids.get(0)));
    assertTrue(fulltextCollection.exists(ids.get(1)));
    assertEquals(new Pair<ZonedDateTime,ZonedDateTime>(ZonedDateTime.of(LocalDateTime.of(2018,Month.JULY,11,14,52,41,794000000),ZoneOffset.UTC),ZonedDateTime.of(LocalDateTime.of(2018,Month.OCTOBER,23,9,5,35,490000000),ZoneOffset.UTC)),fulltextCollection.getLastUpdateDates(ids.get(0)));
    assertEquals(new Pair<ZonedDateTime,ZonedDateTime>(ZonedDateTime.of(LocalDateTime.of(2018,Month.JULY,11,14,54,57,295000000),ZoneOffset.UTC),ZonedDateTime.of(LocalDateTime.of(2018,Month.OCTOBER,23,9,0,52,508000000),ZoneOffset.UTC)),fulltextCollection.getLastUpdateDates(ids.get(1)));
    fulltextCollection.deleteDocument(ids.get(0));
    fulltextCollection.deleteDocument(ids.get(1));
    fulltextCollection.commit();
  }

  public void metadataTest() throws Exception {
    List<String> ids = new ArrayList<>();
    ids.add("/9200396/BibliographicResource_3000118435009");
    ids.add("/9200396/BibliographicResource_3000118436165");


    assertEquals(ZonedDateTime.of(LocalDateTime.of(2018,Month.JULY,11,14,52,41,794000000),ZoneOffset.UTC),metadataCollection.getLastUpdateDate(ids.get(0)));
    assertEquals(ZonedDateTime.of(LocalDateTime.of(2018,Month.JULY,11,14,54,57,295000000),ZoneOffset.UTC), metadataCollection.getLastUpdateDate(ids.get(1)));

    LocalDateTime date = LocalDateTime.of(2022,Month.APRIL,28,15,00,04,0);
    ZonedDateTime dateZone = ZonedDateTime.of(date,ZoneOffset.UTC);

    List<TupleStream> streams = metadataCollection.getDocumentsModifiedAfter(dateZone);
    List<String> documents = metadataCollection.getDocumentsModifiedAfter(streams); //44 documents
  }

  public void synchFulltextTest() throws Exception {
    fulltextCollection.synchronizeFulltextContent(ZonedDateTime.ofInstant(Instant.EPOCH,ZoneOffset.UTC));
    //fulltextCollection.synchronizeFulltextContent();
    assertEquals(new ArrayList<String>(), fulltextCollection.isFulltextUpdated());
  }

  public void synchMetadata() throws IOException, SolrServerException {
    fulltextCollection.synchronizeMetadataContent(ZonedDateTime.ofInstant(Instant.EPOCH,ZoneOffset.UTC));
  }

  public void tests(){
    //fulltextTest();
    //metadataTest();
    //synchFulltextTest();
    //synchMetadata();
  }

  @Override
  public void run(String... args) throws Exception {
    //use:
    //fulltextCollection.synchronizeFulltextContent();
    //fulltextCollection.synchronizeMetadataContent();

    //for intensive check/repair if something goes wrong
    //List<String> toRepair = fulltextCollection.isFulltextUpdated();
    //fulltextCollection.synchronizeFulltextContent(toRepair);
  }
}
