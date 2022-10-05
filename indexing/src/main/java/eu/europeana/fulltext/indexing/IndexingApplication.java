package eu.europeana.fulltext.indexing;

import static eu.europeana.fulltext.indexing.model.IndexingJobType.FULLTEXT_INDEXING;
import static eu.europeana.fulltext.indexing.model.IndexingJobType.METADATA_SYNC;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

@SpringBootApplication(
    scanBasePackages = "eu.europeana.fulltext",
    exclude = {
        SecurityAutoConfiguration.class,

        // disable Spring Mongo auto config
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
    })
public class IndexingApplication implements CommandLineRunner {

  private static final Logger logger = LogManager.getLogger(IndexingApplication.class);
  private static String job = "";
  private static ZonedDateTime modifiedTimestamp;
  @Autowired private IndexingBatchConfig batchConfig;

  public static void main(String[] args) {
    job = args.length > 0 ? args[0]: "";
    modifiedTimestamp = args.length > 1 ? ZonedDateTime.parse(args[1], DateTimeFormatter.ISO_DATE_TIME) : null;
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

  @Override
  public void run(String... args) throws Exception {
    // run fulltext indexing job by default, if no arg is provided
    if (FULLTEXT_INDEXING.value().equalsIgnoreCase(job) || !StringUtils.hasLength(job)) {
      batchConfig.indexFulltext(modifiedTimestamp);
    } else if (METADATA_SYNC.value().equalsIgnoreCase(job)) {
      batchConfig.syncMetadataJob();
    }
  }
}
