/*
 * Copyright 2007-2018 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 *
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */

package eu.europeana.fulltext.loader.config;

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
@PropertySource("classpath:loader.properties")
@PropertySource(value = "classpath:loader.user.properties", ignoreResourceNotFound = true)
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
        datastore.ensureIndexes();
        return datastore;
    }
}
