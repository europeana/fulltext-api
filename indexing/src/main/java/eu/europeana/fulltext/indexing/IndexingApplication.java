package eu.europeana.fulltext.indexing;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.indexing.config.DataSourceConfig;
import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IndexingApplication implements CommandLineRunner {

  @Autowired
  private IndexingAnnoPageRepository repository;

  private static final Logger logger = LogManager.getLogger(IndexingApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(IndexingApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    List<AnnoPage> annoPages = repository.getRecordsModifiedAfter(1654095437);
    logger.info("{}", annoPages);

  }
}
