package eu.europeana.fulltext.annosync.batch;

import eu.europeana.fulltext.AppConstants;
import eu.europeana.fulltext.annosync.config.AnnoSyncSettings;
import eu.europeana.fulltext.annosync.service.EmailService;
import eu.europeana.fulltext.util.GeneralUtils;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class MailSenderTasklet implements Tasklet {

  private final AnnoSyncStats stats;
  private final EmailService emailService;

  private final Instant from;
  private final Instant to;

  private final boolean mailEnabled;

  private final String annotationApiUrl;
  private final String annotationWskey;

  private static final Logger logger = LogManager.getLogger(MailSenderTasklet.class);

  public MailSenderTasklet(
      AnnoSyncStats stats,
      EmailService emailService,
      Instant from,
      Instant to,
      AnnoSyncSettings settings) {
    this.stats = stats;
    this.emailService = emailService;
    this.from = from;
    this.to = to;
    this.annotationApiUrl = settings.getAnnotationsApiUrl();
    this.annotationWskey = settings.getAnnotationsApiKey();
    this.mailEnabled = settings.annoSyncMailEnabled();
  }

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext)
      throws Exception {

    String annotationSearchQuery =
        annotationApiUrl
            + AppConstants.ANNOTATION_SEARCH_PATH
            + "?wskey="
            + annotationWskey
            + "&query="
            + URLEncoder.encode(
                GeneralUtils.generateAnnotationSearchQuery(from, to),
            StandardCharsets.UTF_8);

    if (mailEnabled && stats.getNew() + stats.getUpdated() + stats.getDeprecated() > 0) {
      emailService.sendAnnoSyncSuccessEmail(
          "Successful Annotations Sync", stats,
          annotationSearchQuery);
    } else {
      logger.info(
          "Email not sent. new={}; updated={}; deleted={}; mailEnabled={}",
          stats.getNew(),
          stats.getUpdated(),
          stats.getDeprecated(),
          mailEnabled);
    }
    return RepeatStatus.FINISHED;
  }
}
