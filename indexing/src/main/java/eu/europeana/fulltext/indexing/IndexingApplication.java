package eu.europeana.fulltext.indexing;

import eu.europeana.fulltext.entity.AnnoPage;
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

  @Override
  public void run(String... args) throws Exception {
    //use:
    //fulltextCollection.synchronizeFulltextContent();
    //fulltextCollection.synchronizeMetadataContent();  //we can get teh last update in the fulltext collection, but it may be misleading as it could come from a new record added
    List<String> toRepair = fulltextCollection.isFulltextUpdated();

    //for intensive check/repair if something goes wrong
    //List<String> toRepair = fulltextCollection.isFulltextUpdated();
    //fulltextCollection.synchronizeFulltextContent(toRepair);
  }
}
