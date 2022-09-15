package eu.europeana.fulltext.migrations.config;

import static eu.europeana.fulltext.migrations.MigrationConstants.FULLTEXT_DEST_DATASTORE;
import static eu.europeana.fulltext.migrations.MigrationConstants.FULLTEXT_SRC_DATASTORE;
import static eu.europeana.fulltext.util.MorphiaUtils.MAPPER_OPTIONS;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import eu.europeana.fulltext.migrations.model.AtomicReferenceCodec;
import eu.europeana.fulltext.migrations.model.AtomicReferenceCodecProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MigrationDatasourceConfig {
  private final MigrationAppSettings config;
  private static final Logger logger = LogManager.getLogger(MigrationDatasourceConfig.class);
  private Datastore sourceMongoDataStore;

  public MigrationDatasourceConfig(MigrationAppSettings config) {
    this.config = config;
  }

  public MongoClient createMongoClient(String connectionUrl, boolean addCodecProvider) {

    Builder mongoBuilder =
        MongoClientSettings.builder().applyConnectionString(new ConnectionString(connectionUrl));

    if (addCodecProvider) {
      // Configure custom codecs
      CodecProvider pojoCodecProvider =
          PojoCodecProvider.builder().register(new AtomicReferenceCodecProvider()).build();

      CodecRegistry codecRegistry =
          CodecRegistries.fromRegistries(
              CodecRegistries.fromCodecs(new AtomicReferenceCodec()),
              CodecRegistries.fromProviders(pojoCodecProvider),
              MongoClientSettings.getDefaultCodecRegistry());
      mongoBuilder.codecRegistry(codecRegistry);
    }

    return MongoClients.create(mongoBuilder.build());
  }

  @Bean(FULLTEXT_SRC_DATASTORE)
  @Primary
  public Datastore fulltextSourceDb() {
    String fulltextDatabase = config.getFulltextSrcDatabase();
    logger.info("Configuring fulltext database: {}", fulltextDatabase);

    // if using same db as source and dest, we configure custom codecs when creating this datastore
    return Morphia.createDatastore(
        createMongoClient(config.getMongoSrcConnectionUrl(), config.useSameDb()),
        fulltextDatabase,
        MAPPER_OPTIONS);
  }

  @Bean(FULLTEXT_DEST_DATASTORE)
  public Datastore fulltextDestDb(
      @Qualifier((FULLTEXT_SRC_DATASTORE)) Datastore srcDataStore) {

    if (config.useSameDb()) {
      logger.info("Using the same Mongo db as source and destination");
      return srcDataStore;
    }

    String fulltextDatabase = config.getFulltextDestDatabase();
    logger.info("Configuring destination  database: {}", fulltextDatabase);

    // if using different DBs, we write JobMetadata to the dest db. So custom codecs need to be configured
    return Morphia.createDatastore(
        createMongoClient(config.getMongoDestConnectionUrl(), !config.useSameDb()),
        fulltextDatabase,
        MAPPER_OPTIONS);
  }
}
