package eu.europeana.fulltext.api.config;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.apache.logging.log4j.LogManager;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
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
    public AdvancedDatastore datastore(MongoClient mongoClient, MongoProperties mongoProperties) {
        // There can be an alternative database defined via spring.data.mongodb.database, so check if that's the case
        String database = mongoProperties.getDatabase();
        MongoClientURI uri = new MongoClientURI(mongoProperties.getUri());
        if (StringUtils.isEmpty(database)) {
            database = uri.getDatabase();
        }
        LogManager.getLogger(eu.europeana.fulltext.api.config.DataSourceConfig.class).
                info("Connecting to {} Mongo database on hosts {}...", database, uri.getHosts());
        final AdvancedDatastore datastore = (AdvancedDatastore) new Morphia().createDatastore(mongoClient, database);
        return datastore;
    }
}
