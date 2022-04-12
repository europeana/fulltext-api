package eu.europeana.fulltext.api.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import eu.europeana.batch.entity.PackageMapper;
import eu.europeana.fulltext.util.MorphiaUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import static eu.europeana.fulltext.AppConstants.FULLTEXT_DATASTORE_BEAN;
import static eu.europeana.fulltext.AppConstants.SPRINGBATCH_DATASTORE_BEAN;
import static eu.europeana.fulltext.util.MorphiaUtils.MAPPER_OPTIONS;

/**
 * Created by luthien on 01/10/2018.
 */

@Configuration
public class DataSourceConfig {

    private final FTSettings settings;
    private static final Logger logger = LogManager.getLogger(DataSourceConfig.class);


    public DataSourceConfig(FTSettings settings) {
        this.settings = settings;
    }


    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(settings.getMongoConnectionUrl());
    }

    @Bean(FULLTEXT_DATASTORE_BEAN)
    @Primary
    public Datastore fulltextDatastore(MongoClient mongoClient) {
        String fulltextDatabase = settings.getFulltextDatabase();
        logger.info("Configuring fulltext database: {}", fulltextDatabase);

        // TODO ensure indexes when TranslationAnnoPage no longer inherits index definitions from AnnoPage
        return Morphia.createDatastore(mongoClient, fulltextDatabase, MorphiaUtils.MAPPER_OPTIONS);
    }

    @Bean(SPRINGBATCH_DATASTORE_BEAN)
    public Datastore batchDatastore(MongoClient mongoClient) {
        String batchDatabase = settings.getBatchDatabase();

        logger.info("Configuring Batch database: {}", batchDatabase);
        Datastore datastore = Morphia.createDatastore(mongoClient, batchDatabase);
        // Indexes aren't created unless Entity classes are explicitly mapped.
        datastore.getMapper().mapPackage(PackageMapper.class.getPackageName());
        datastore.ensureIndexes();
        return datastore;
    }
}
