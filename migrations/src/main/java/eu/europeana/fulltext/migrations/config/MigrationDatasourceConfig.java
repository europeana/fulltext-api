package eu.europeana.fulltext.migrations.config;

import static eu.europeana.fulltext.migrations.MigrationConstants.BATCH_DATASTORE_BEAN;
import static eu.europeana.fulltext.migrations.MigrationConstants.FULLTEXT_DATASTORE_BEAN;
import static eu.europeana.fulltext.util.MorphiaUtils.MAPPER_OPTIONS;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import eu.europeana.batch.entity.PackageMapper;
import eu.europeana.fulltext.migrations.model.AtomicReferenceCodec;
import eu.europeana.fulltext.migrations.model.AtomicReferenceCodecProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MigrationDatasourceConfig {
  private final MigrationAppSettings config;
  private static final Logger logger = LogManager.getLogger(MigrationDatasourceConfig.class);

  public MigrationDatasourceConfig(MigrationAppSettings config) {
    this.config = config;
  }

  @Bean
  public MongoClient mongoClient() {
    // Configure custom codecs
    CodecProvider pojoCodecProvider =
        PojoCodecProvider.builder().register(new AtomicReferenceCodecProvider()).build();

    CodecRegistry codecRegistry =
        CodecRegistries.fromRegistries(
            CodecRegistries.fromCodecs(new AtomicReferenceCodec()),
            CodecRegistries.fromProviders(pojoCodecProvider),
            MongoClientSettings.getDefaultCodecRegistry());

    return MongoClients.create(
        MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(config.getMongoConnectionUrl()))
            .codecRegistry(codecRegistry)
            .build());
  }

  @Bean(FULLTEXT_DATASTORE_BEAN)
  @Primary
  public Datastore fulltextDatastore(MongoClient mongoClient) {
    String fulltextDatabase = config.getFulltextDatabase();
    logger.info("Configuring fulltext database: {}", fulltextDatabase);

    return Morphia.createDatastore(mongoClient, fulltextDatabase, MAPPER_OPTIONS);
  }

  @Bean(BATCH_DATASTORE_BEAN)
  public Datastore batchDatastore(MongoClient mongoClient) {
    String batchDatabase = config.getBatchDatabase();

    logger.info("Configuring Batch database: {}", batchDatabase);
    Datastore datastore = Morphia.createDatastore(mongoClient, batchDatabase);
    // Indexes aren't created unless Entity classes are explicitly mapped.
    datastore.getMapper().mapPackage(PackageMapper.class.getPackageName());
    datastore.ensureIndexes();
    return datastore;
  }
}
