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
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

@EnableBatchProcessing
@SpringBootApplication(
    scanBasePackages = "eu.europeana.fulltext",
    exclude = {SecurityAutoConfiguration.class})
public class IndexingApplication {
  public static void main(String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(IndexingApplication.class, args);
    System.exit(SpringApplication.exit(context));
  }


  public void run(String... args) throws Exception {
    // use:
    // fulltextCollection.synchronizeFulltextContent();
    // fulltextCollection.synchronizeMetadataContent();
    // List<String> toRepair = fulltextCollection.isFulltextUpdated();

    // for intensive check/repair if something goes wrong
    // List<String> toRepair = fulltextCollection.isFulltextUpdated();
    //    fulltextCollection.synchronizeFulltextContent(toRepair);
  }
}
