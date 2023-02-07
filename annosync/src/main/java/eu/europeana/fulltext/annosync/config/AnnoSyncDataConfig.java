package eu.europeana.fulltext.annosync.config;

import static eu.europeana.fulltext.util.MorphiaUtils.MAPPER_OPTIONS;

import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnnoSyncDataConfig {

  private static final Logger logger = LogManager.getLogger(AnnoSyncDataConfig.class);
  private final AnnoSyncSettings settings;

  public AnnoSyncDataConfig(AnnoSyncSettings settings) {
    this.settings = settings;
  }

  @Bean
  public Datastore fulltextDatastore() {
    String fulltextDatabase = settings.getFulltextDatabase();
    logger.info("Configuring fulltext database: {}", fulltextDatabase);

    return Morphia.createDatastore(MongoClients.create(settings.getMongoConnectionUrl()),
        fulltextDatabase,
        MAPPER_OPTIONS);

  }
}
