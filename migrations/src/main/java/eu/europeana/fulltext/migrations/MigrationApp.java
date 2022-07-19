package eu.europeana.fulltext.migrations;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

@EnableBatchProcessing
@SpringBootApplication(
    scanBasePackages = "eu.europeana.fulltext",
    exclude = {
      // Disabled as we're using our own auth mechanism
      SecurityAutoConfiguration.class
    })
public class MigrationApp {
  public static void main(String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(MigrationApp.class, args);
    System.exit(SpringApplication.exit(context));
  }
}
