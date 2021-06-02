package eu.europeana.fulltext.api.config;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.api.service.ControllerUtils;
import eu.europeana.fulltext.search.exception.InvalidParameterException;
import eu.europeana.fulltext.search.web.FTSearchController;
import io.micrometer.core.instrument.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Contains settings from fulltext.properties and fulltext.user.properties files
 * @author Lúthien
 * Created on 31/05/2018
 */
@Configuration
@Component
@PropertySource("classpath:fulltext.properties")
@PropertySource(value = "classpath:fulltext.user.properties", ignoreResourceNotFound = true)
public class FTSettings {

    private static final Logger LOG = LogManager.getLogger(FTSettings.class);

    private Boolean suppressParseException = false; // default value if we run this outside of Spring

    @Value("${annopage.baseurl}")
    private String annoPageBaseUrl;

    @Value("${annotation.baseurl}")
    private String annotationBaseUrl;

    @Value("${resource.baseurl}")
    private String resourceBaseUrl;

    @Value("${search.textGranularity.default:Word}")
    private String searchTextGranularity;
    private List<AnnotationType> defaultSearchTextGranularity;

    @Autowired
    private Environment environment;


    @PostConstruct
    private void init() throws InvalidParameterException {
        if (StringUtils.isBlank(searchTextGranularity)) {
            LOG.info("No default text granularity for search found in configuration files");
            defaultSearchTextGranularity = Arrays.asList(AnnotationType.BLOCK, AnnotationType.LINE, AnnotationType.WORD);
        } else {
            defaultSearchTextGranularity = ControllerUtils.validateTextGranularity(searchTextGranularity,
                    FTSearchController.ALLOWED_ANNOTATION_TYPES);
        }
        LOG.info("Default text granularity for search = {}", searchTextGranularity);
    }

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
        } catch (IOException e) {
            LogManager.getLogger(FTSettings.class).warn("Error loading build.properties", e);
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

    public List<AnnotationType> getDefaultSearchTextGranularity() {
        return Collections.unmodifiableList(defaultSearchTextGranularity);
    }
}
