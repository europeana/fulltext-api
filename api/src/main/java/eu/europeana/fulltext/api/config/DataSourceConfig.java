package eu.europeana.fulltext.api.config;

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

/**
 * Created by luthien on 01/10/2018.
 */

@Configuration
@PropertySource("classpath:fulltext.properties")
@PropertySource(value = "classpath:fulltext.user.properties", ignoreResourceNotFound = true)
public class DataSourceConfig {

    @Bean
    public Datastore datastore(MongoClient mongoClient, MongoProperties mongoProperties) {
        // There can be an alternative database defined via spring.data.mongodb.database, so check if that's the case
        String database = mongoProperties.getDatabase();
        MongoClientURI uri = new MongoClientURI(mongoProperties.getUri());
        if (StringUtils.isEmpty(database)) {
            database = uri.getDatabase();
        }
        LogManager.getLogger(DataSourceConfig.class).
                info("Connecting to {} Mongo database on hosts {}...", database, uri.getHosts());

        return Morphia.createDatastore(mongoClient, database);
    }
}
