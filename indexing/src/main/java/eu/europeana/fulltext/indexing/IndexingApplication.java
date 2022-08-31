package eu.europeana.fulltext.indexing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
public class IndexingApplication implements CommandLineRunner {

  private static final Logger logger = LogManager.getLogger(IndexingApplication.class);

  @Autowired private IndexingBatchConfig batchConfig;

  private static final String FULLTEXT_INDEX_ARG = "fulltextIndex";
  private static final String METADATA_SYNC_ARG = "metadataSync";

  private static String job = "";

  @Override
  public void run(String... args) throws Exception {

    if (FULLTEXT_INDEX_ARG.equalsIgnoreCase(job)) {
      batchConfig.indexFulltext();
    } else if (METADATA_SYNC_ARG.equalsIgnoreCase(job)) {
      batchConfig.syncMetadataJob();
    }
  }

  public static void main(String[] args) {
    job = args[0];
    if (!FULLTEXT_INDEX_ARG.equalsIgnoreCase(job) && !METADATA_SYNC_ARG.equalsIgnoreCase(job)) {
      logger.error(
          "Unsupported argument '{}'. Supported arguments are '{}' and '{}'",
          job,
          FULLTEXT_INDEX_ARG,
          METADATA_SYNC_ARG);
      System.exit(1);
    }

    ConfigurableApplicationContext context = SpringApplication.run(IndexingApplication.class, args);
    System.exit(SpringApplication.exit(context));
  }
}
