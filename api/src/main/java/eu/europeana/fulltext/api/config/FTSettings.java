package eu.europeana.fulltext.api.config;

import static eu.europeana.fulltext.util.GeneralUtils.testProfileNotActive;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.api.service.ControllerUtils;
import eu.europeana.fulltext.search.exception.InvalidParameterException;
import eu.europeana.fulltext.search.web.FTSearchController;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

/**
 * Contains settings from fulltext.properties and fulltext.user.properties files
 * @author Lúthien
 * Created on 31/05/2018
 */
@Configuration
@PropertySource("classpath:fulltext.properties")
@PropertySource(value = "classpath:fulltext.user.properties", ignoreResourceNotFound = true)
public class FTSettings implements InitializingBean {

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


    @Value("${spring.profiles.active:}")
    private String activeProfileString;


    @Value("${auth.enabled}")
    private boolean authEnabled;

    @Value("${europeana.apikey.jwttoken.signaturekey}")
    private String apiKeyPublicKey;

    @Value("${authorization.api.name}")
    private String authorizationApiName;

    @Value("${europeana.apikey.serviceurl}")
    private String apiKeyUrl;

    @Value("${mongo.connectionUrl}")
    private String mongoConnectionUrl;

    @Value("${mongo.fulltext.database}")
    private String fulltextDatabase;

    @Value("${mongo.fulltext.ensureIndices: false}")
    private boolean ensureFulltextIndices;

    @Value("${annotations.serviceurl}")
    private String annotationsApiUrl;

    @Value("${annotations.id.hosts}")
    private String annotationIdHostsPattern;

    @Value("${annotations.wskey}")
    private String annotationsApiKey;

    @Value("${batch.annotations.pageSize: 50}")
    private int annotationItemsPageSize;

    @Value("${batch.executor.corePool: 5}")
    private int batchCorePoolSize;

    @Value("${batch.step.skipLimit: 10}")
    private int batchSkipLimit;

    @Value("${batch.executor.maxPool: 10}")
    private int batchMaxPoolSize;

    @Value("${batch.step.executor.queueSize: 5}")
    private int batchQueueSize;

    @Value("${batch.step.throttleLimit: 5}")
    private int annoSyncThrottleLimit;

    @Value("${annosync.initialDelaySeconds}")
    private int annoSyncInitialDelay;

    @Value("${annosync.intervalSeconds}")
    private int annoSyncInterval;

    @Value("${spring.data.solr.repositories.enabled}")
    private boolean solrEnabled;

    @Value("${annosync.enabled}")
    private boolean annoSyncEnabled;

    @Value("${annosync.mail.enabled}")
    private boolean annoSyncMailEnabled;

    @Value("${annosync.mail.from:}")
    private String mailFrom;

    @Value("${annosync.mail.to:}")
    private String mailTo;

    @Value("${annosync.mail.cc:}")
    private String mailCc;

    @Value("${fulltext.deployment:}")
    private String deploymentName;

    @Value("${webclient.maxBufferMb:16}")
    private int maxBufferMb;

    @Value("${annotations.retry:3}")
    private int retryLimit;

    @Autowired
    private Environment environment;

    public boolean isAuthEnabled() {
        return authEnabled;
    }

    public String getAuthorizationApiName() {
        return authorizationApiName;
    }

    public String getApiKeyPublicKey() {
        return apiKeyPublicKey;
    }

    public String getApiKeyUrl() {
        return apiKeyUrl;
    }

    public String getMongoConnectionUrl() {
        return mongoConnectionUrl;
    }

    public String getFulltextDatabase() {
        return fulltextDatabase;
    }

    public boolean ensureFulltextIndices() {
        return ensureFulltextIndices;
    }

    public String getAnnotationsApiKey() {
        return annotationsApiKey;
    }

    public int getRetryLimit() {
        return retryLimit;
    }

    public String getAnnotationsApiUrl() {
        return annotationsApiUrl;
    }

    public int getAnnotationItemsPageSize() {
        return annotationItemsPageSize;
    }

    public int getBatchCorePoolSize() {
        return batchCorePoolSize;
    }

    public int getBatchMaxPoolSize() {
        return batchMaxPoolSize;
    }

    public int getBatchQueueSize() {
        return batchQueueSize;
    }

    public int getAnnoSyncInitialDelay() {
        return annoSyncInitialDelay;
    }

    public int getAnnoSyncInterval() {
        return annoSyncInterval;
    }

    public int getAnnoSyncThrottleLimit() {
        return annoSyncThrottleLimit;
    }

    public String getAnnotationIdHostsPattern() {
        return annotationIdHostsPattern;
    }


    private void setupTextGranularity() throws InvalidParameterException {
        if (StringUtils.isBlank(searchTextGranularity)) {
            LOG.info("No default text granularity for search found in configuration files");
            defaultSearchTextGranularity = Arrays.asList(AnnotationType.LINE);
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

    @Override
    public void afterPropertiesSet() throws Exception {
        if (testProfileNotActive(activeProfileString)) {
            validateRequiredSettings();
        }

        setupTextGranularity();
    }

    private void validateRequiredSettings() {
        List<String> missingProps = new ArrayList<>();
        // single validations
        Map<String, String> singleValidations = new HashMap<>();
        singleValidations.put(annotationsApiKey, "annotations.wskey");
        singleValidations.put(apiKeyPublicKey, "europeana.apikey.jwttoken.signaturekey");
        singleValidations.put(apiKeyUrl, "europeana.apikey.serviceurl");
        singleValidations.put(annotationsApiUrl, "annotations.serviceurl");

        // validate all
        validateValues(singleValidations, missingProps);

        // group validations
        if (authEnabled) {
            validateValues(Map.of(authorizationApiName, "authorization.api.name",
                    apiKeyUrl , "europeana.apikey.serviceurl",
                    apiKeyPublicKey, "europeana.apikey.jwttoken.signaturekey"), missingProps);
        }
        if (annoSyncEnabled) {
            validateValues(Map.of(deploymentName, "fulltext.deployment"), missingProps);
        }

        if (!missingProps.isEmpty()) {
            throw new IllegalStateException(
                String.format(
                    "The following config properties are not set: %s",
                    String.join("\n", missingProps)));
        }
    }

    private void validateValues(Map<String, String> map, List<String> missingProps) {
        for (Map.Entry<String,  String> entry : map.entrySet()) {
            if (StringUtils.isEmpty(entry.getKey())) {
                missingProps.add(entry.getValue());
            }
        }
    }

    public boolean isSolrEnabled() {
        return solrEnabled;
    }

    public boolean isAnnoSyncEnabled() {
        return annoSyncEnabled;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public String getMailTo() {
        return mailTo;
    }

    public String getMailCc() {
        return mailCc;
    }

    public String getDeploymentName(){
        return deploymentName;
    }

    public boolean annoSyncMailEnabled() {
        return annoSyncMailEnabled;
    }

    public int getMaxBufferMb() {
        return maxBufferMb;
    }

    public int getSkipLimit() {
        return batchSkipLimit;
    }
}
