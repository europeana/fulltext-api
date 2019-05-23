package eu.europeana.fulltext.api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Properties;

/**
 * Contains settings from fulltext.properties and fulltext.user.properties files
 * @author Lúthien
 * Created on 31/05/2018
 */
@Configuration
@Component
@PropertySource("classpath:fulltext.properties")
@PropertySource(value = "classpath:fulltext.user.properties", ignoreResourceNotFound = true)
//@EnableMongoRepositories(basePackages="eu.europeana.fulltext")
public class FTSettings {
    private Boolean suppressParseException = false; // default value if we run this outside of Spring

    @Value("${annopage.baseurl}")
    private String annoPageBaseUrl;

    @Value("${annotation.baseurl}")
    private String annotationBaseUrl;

    @Value("${resource.baseurl}")
    private String resourceBaseUrl;

    @Value("${annopage.directory}")
    private String annoPageDirectory;

    @Value("${annotation.directory}")
    private String annotationDirectory;

    @Autowired
    private Environment environment;


    /**
     * For production we want to suppress exceptions that arise from parsing record data, but for testing/debugging we
     * want to see those exceptions
     * @return
     */
    public Boolean getSuppressParseException() {
        return suppressParseException;
    }

    /**
     * Note: this does not work when running the exploded build from the IDE because the values in the build.properties
     * are substituted only in the .war file. It returns 'default' in that case.
     * @return String containing app version, used in the eTag SHA hash generation
     */
    public String getAppVersion() {
        Properties  buildProperties  = new Properties();
        InputStream resourceAsStream = this.getClass().getResourceAsStream("/build.properties");
        try {
            buildProperties.load(resourceAsStream);
            return environment.getProperty("info.app.version");
        } catch (Exception e) {
            return "default";
        }
    }

    public String getAnnoPageBaseUrl() {
        return annoPageBaseUrl;
    }

    public String getAnnotationBaseUrl() {
        return annotationBaseUrl;
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

}
