package eu.europeana.fulltext.indexing;

import static eu.europeana.fulltext.indexing.model.IndexingJobType.FULLTEXT_INDEXING;
import static eu.europeana.fulltext.indexing.model.IndexingJobType.METADATA_SYNC;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

@EnableBatchProcessing
@SpringBootApplication(
    scanBasePackages = "eu.europeana.fulltext",
    exclude = {SecurityAutoConfiguration.class})
public class IndexingApplication implements CommandLineRunner {

  private static final Logger logger = LogManager.getLogger(IndexingApplication.class);

  @Autowired private IndexingBatchConfig batchConfig;

  private static String job = "";

  @Override
  public void run(String... args) throws Exception {
    // run fulltext indexing job by default, if no arg is provided
    if (FULLTEXT_INDEXING.value().equalsIgnoreCase(job) || !StringUtils.hasLength(job)) {
      batchConfig.indexFulltext();
    } else if (METADATA_SYNC.value().equalsIgnoreCase(job)) {
      batchConfig.syncMetadataJob();
    }
  }

  public static void main(String[] args) {
    job = args.length > 0 ?args[0]: "";
    validateArgs();

    ConfigurableApplicationContext context = SpringApplication.run(IndexingApplication.class, args);
    System.exit(SpringApplication.exit(context));
  }

  private static void validateArgs() {
    if (StringUtils.hasLength(job)
        && !FULLTEXT_INDEXING.value().equalsIgnoreCase(job)
        && !METADATA_SYNC.value().equalsIgnoreCase(job)) {
      logger.error(
          "Unsupported argument '{}'. Supported arguments are '{}' and '{}'",
          job,
          FULLTEXT_INDEXING.value(),
          METADATA_SYNC.value());
      System.exit(1);
    }
  }
}
