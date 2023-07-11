package eu.europeana.fulltext.annosync.config;

import eu.europeana.fulltext.AppConstants;
import eu.europeana.fulltext.CommonBeanConfig;
import eu.europeana.fulltext.service.AnnotationApiRestService;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class AnnoSyncBeanConfig {

  private final AnnoSyncSettings settings;

  public AnnoSyncBeanConfig(AnnoSyncSettings settings) {
    this.settings = settings;
  }


  @Bean
  @Primary
  public SpringTemplateEngine springTemplateEngine() {
    SpringTemplateEngine springTemplateEngine = new SpringTemplateEngine();
    springTemplateEngine.addTemplateResolver(emailTemplateResolver());
    return springTemplateEngine;
  }


  @Bean
  public AnnotationApiRestService annotationApiRestService() {
    return new AnnotationApiRestService(CommonBeanConfig.createWebClient(
        Optional.ofNullable(settings.getAnnotationsApiUrl()), settings.getMaxBufferMb()),
        settings.getAnnotationsApiKey());
  }

  private ClassLoaderTemplateResolver emailTemplateResolver() {
    ClassLoaderTemplateResolver emailTemplateResolver = new ClassLoaderTemplateResolver();
    emailTemplateResolver.setPrefix("/templates/");
    emailTemplateResolver.setSuffix(".html");
    emailTemplateResolver.setTemplateMode(TemplateMode.HTML);
    emailTemplateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
    emailTemplateResolver.setCacheable(false);
    return emailTemplateResolver;
  }


  /**
   * Task executor used by the Spring Batch job launcher. Since jobs are launched via Spring
   * Scheduling, this returns a SyncTaskExecutor – so scheduling blocks while jobs are running.
   */
  @Bean(AppConstants.JOB_LAUNCHER_TASK_EXECUTOR)
  public TaskExecutor jobLauncherTaskExecutor() {
    // launch all Spring Batch jobs within the Spring Scheduling thread
    return new SyncTaskExecutor();
  }

  /** Task executor used by the Spring Batch step for multi-threading */
  @Bean(AppConstants.ANNO_SYNC_TASK_EXECUTOR)
  public TaskExecutor annoSyncTaskExecutor() {
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(settings.getBatchCorePoolSize());
    taskExecutor.setMaxPoolSize(settings.getBatchMaxPoolSize());
    taskExecutor.setQueueCapacity(settings.getBatchQueueSize());

    return taskExecutor;
  }
}
