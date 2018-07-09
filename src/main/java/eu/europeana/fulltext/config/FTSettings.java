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

package eu.europeana.fulltext.config;

import com.mongodb.MongoClient;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;


/**
 * Contains settings from fulltext.properties and fulltext.user.properties files
 * and does the Morphia initialisation
 * @author Lúthien
 * Created on 31/05/2018
 */
@Configuration
@Component
@PropertySource("classpath:fulltext.properties")
@PropertySource(value = "classpath:fulltext.user.properties", ignoreResourceNotFound = true)
public class FTSettings {

    private Boolean suppressParseException = false; // default value if we run this outside of Spring

    @Value("${iiifapi.baseurl}")
    private String iiifApiBaseUrl;

    @Value("${resource.baseurl}")
    private String resourceBaseUrl;

    @Value("${annopage.directory}")
    private String annoPageDirectory;

    @Value("${annotation.directory}")
    private String annotationDirectory;

    @Value("${target.directory}")
    private String targetDirectory;


    // Inject an instance of Spring-Boot MongoProperties
    @Autowired
    private MongoProperties mongoProperties;

    private Morphia morphia() {
        final Morphia morphia = new Morphia();
        // tell Morphia where to find your classes
        // can be called multiple times with different packages or classes
        morphia.mapPackage("eu.europeana.fulltext.entity");

        return morphia;
    }

    @Bean
    public Datastore datastore(MongoClient mongoClient) {
        // create the Datastore connecting to the default port on the local host
        final Datastore datastore = morphia().createDatastore(mongoClient, mongoProperties.getDatabase());
        datastore.ensureIndexes();

        return datastore;
    }

    /**
     * For production we want to suppress exceptions that arise from parsing record data, but for testing/debugging we
     * want to see those exceptions
     * @return
     */
    public Boolean getSuppressParseException() {
        return suppressParseException;
    }

    public String getIiifApiBaseUrl() {
        return iiifApiBaseUrl;
    }

    public String getResourceBaseUrl() {
        return resourceBaseUrl;
    }

    public String getAnnoPageDirectory() {
        return annoPageDirectory;
    }

    public String getAnnotationDirectory() {
        return annotationDirectory;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }
}
