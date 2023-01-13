package eu.europeana.fulltext.annosync.config;

import static eu.europeana.fulltext.util.GeneralUtils.testProfileNotActive;
import static eu.europeana.fulltext.util.SettingsUtils.validateValues;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:annosync.properties")
@PropertySource(value = "classpath:annosync.user.properties", ignoreResourceNotFound = true)
public class AnnoSyncSettings implements InitializingBean {

  @Value("${mongo.connectionUrl}")
  private String mongoConnectionUrl;
  @Value("${mongo.fulltext.database}")
  private String fulltextDatabase;

  @Value("${annotations.wskey}")
  private String annotationsApiKey;


  @Value("${annotations.serviceurl}")
  private String annotationsApiUrl;

  @Value("${spring.profiles.active:}")
  private String activeProfileString;

  @Value("${batch.annotations.pageSize: 100}")
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

  public int getAnnoSyncThrottleLimit() {
    return annoSyncThrottleLimit;
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

  public String getDeploymentName() {
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

  public String getAnnotationsApiKey() {
    return annotationsApiKey;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (testProfileNotActive(activeProfileString)) {
      validateRequiredSettings();
    }
  }

  private void validateRequiredSettings() {
    List<String> missingProps = new ArrayList<>();
    Map<String, String> singleValidations = new HashMap<>();

    singleValidations.put(annotationsApiKey, "annotations.wskey");
    singleValidations.put(annotationsApiUrl, "annotations.serviceurl");

    validateValues(Map.of(deploymentName, "fulltext.deployment"), missingProps);
    validateValues(singleValidations, missingProps);

    if (!missingProps.isEmpty()) {
      throw new IllegalStateException(
          String.format(
              "The following config properties are not set: %s",
              String.join("\n", missingProps)));
    }
  }

  public String getMongoConnectionUrl() {
    return mongoConnectionUrl;
  }

  public String getFulltextDatabase() {
    return fulltextDatabase;
  }

}

