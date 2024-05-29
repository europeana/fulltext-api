package eu.europeana.fulltext.loader.config;

import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoClient;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import org.apache.logging.log4j.LogManager;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.StringUtils;

import static eu.europeana.fulltext.util.MorphiaUtils.MAPPER_OPTIONS;

/**
 * Created by luthien on 01/10/2018.
 * @deprecated since 2023
 */
@Deprecated
@Configuration
@PropertySource("classpath:loader.properties")
@PropertySource(value = "classpath:loader.user.properties", ignoreResourceNotFound = true)
public class DataSourceConfig {

    @Bean
    public Datastore datastore(MongoClient mongoClient, MongoProperties mongoProperties) {
        // There can be an alternative database defined via spring.data.mongodb.database, so check if that's the case
        String database = mongoProperties.getDatabase();
        MongoClientURI uri = new MongoClientURI(mongoProperties.getUri());
        if (StringUtils.isEmpty(database)) {
            database = uri.getDatabase();
        }
        LogManager.getLogger(eu.europeana.fulltext.api.config.DataSourceConfig.class).
                info("Connecting to {} Mongo database on hosts {}...", database, uri.getHosts());
        final Datastore datastore = Morphia.createDatastore(mongoClient, database, MAPPER_OPTIONS);
        datastore.ensureIndexes();
        return datastore;
    }
}
