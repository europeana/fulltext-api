package eu.europeana.fulltext.loader.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * Contains settings from loader.properties and loader.user.properties files
 * @author Lúthien
 * @deprecated since 2023
 * Created on 31/05/2018
 */
@Deprecated
@Configuration
@Component
@PropertySource("classpath:loader.properties")
@PropertySource(value = "classpath:loader.user.properties", ignoreResourceNotFound = true)
public class LoaderSettings {

    @Value("${resource.baseurl}")
    private String resourceBaseUrl;

    @Value("${batch.base.directory}")
    private String batchBaseDirectory;

    @Value("${stop.error.save}")
    private Boolean stopOnSaveError;

    public String getResourceBaseUrl() {
        return resourceBaseUrl;
    }

    public String getBatchBaseDirectory() { return batchBaseDirectory; }

    /**
     * @return true if the loader should stop the current loading process when there is an error saving data to Mongo
     */
    public Boolean isStopOnSaveError() {
        return stopOnSaveError;
    }

}
