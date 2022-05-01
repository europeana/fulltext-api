package eu.europeana.fulltext.api.service;

import eu.europeana.fulltext.api.config.FTSettings;
import java.util.Map;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

@Component
public class EmailService {

  private final String from;
  private final String to;
  private final String cc;

  private final String deploymentName;

  private final SpringTemplateEngine thymeleafTemplateEngine;

  private static final Logger logger = LogManager.getLogger(EmailService.class);

  private final JavaMailSender emailSender;

  /** Creates a new EmailService instance. Uses the default Spring Boot JavaMailSender */
  public EmailService(
      @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") JavaMailSender emailSender,
      FTSettings settings,
      SpringTemplateEngine thymeleafTemplateEngine) {
    this.emailSender = emailSender;
    this.from = settings.getMailFrom();
    this.to = settings.getMailTo();
    this.cc = settings.getMailCc();
    this.deploymentName = settings.getDeploymentName();
    this.thymeleafTemplateEngine = thymeleafTemplateEngine;
  }

  private void sendHtmlMessage(String subject, String body) {
    try {
      MimeMessage message = emailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(to);
      helper.setFrom(from);

      if (StringUtils.hasLength(cc)) {
        helper.setCc(cc);
      }
      helper.setSubject(subject);
      helper.setText(body, true);
      emailSender.send(message);
    } catch (MessagingException e) {
      logger.warn("Error sending email message: body={}", body, e);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Sent email to {}; subject={}; body={}", to, subject, body);
    }
  }

  public void sendAnnoSyncSuccessEmail(String subject, int newAnnopages, int updatedAnnopages, int deletedAnnopages, String searchQuery) {

    Context thymeleafContext = new Context();

    Map<String, Object> templateModel =
        Map.of(
            "numNewAnnopages", newAnnopages,
            "numUpdatedAnnopages", updatedAnnopages,
            "numDeletedAnnopages", deletedAnnopages,
            "annotationSearchQuery", searchQuery,
            "deploymentName", deploymentName);

    thymeleafContext.setVariables(templateModel);
    String htmlBody = thymeleafTemplateEngine.process("annosync-success.html", thymeleafContext);

    sendHtmlMessage(subject, htmlBody);
  }
}
