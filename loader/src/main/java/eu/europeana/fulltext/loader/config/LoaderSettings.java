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

import eu.europeana.fulltext.loader.service.MongoSaveMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Component;


/**
 * Contains settings from fulltext.properties and fulltext.user.properties files
 * @author Lúthien
 * Created on 31/05/2018
 */
@Configuration
@Component
@PropertySource("classpath:loader.properties")
@PropertySource(value = "classpath:loader.user.properties", ignoreResourceNotFound = true)
@EnableMongoRepositories(basePackages="eu.europeana.fulltext")
public class LoaderSettings {

    @Value("${resource.baseurl}")
    private String resourceBaseUrl;

    @Value("${batch.base.directory}")
    private String batchBaseDirectory;

    @Value("${spring.data.mongodb.database}")
    private String mongoDbName;

    @Value("${spring.data.mongodb.host}")
    private String mongoHost;


    @Autowired
    MongoDbFactory      mongoDbFactory;

    @Autowired
    MongoMappingContext mongoMappingContext;

    @Bean
    public MappingMongoConverter mappingMongoConverter() {

        DbRefResolver         dbRefResolver = new DefaultDbRefResolver(mongoDbFactory);
        MappingMongoConverter converter     = new MappingMongoConverter(dbRefResolver, mongoMappingContext);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return converter;
    }

    public String getResourceBaseUrl() {
        return resourceBaseUrl;
    }

    public String getBatchBaseDirectory() { return batchBaseDirectory; }

}
