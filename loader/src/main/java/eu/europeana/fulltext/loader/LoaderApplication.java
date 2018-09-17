package eu.europeana.fulltext.loader;

import eu.europeana.fulltext.loader.config.LoaderSettings;
import eu.europeana.fulltext.loader.service.LoadArchiveService;
import eu.europeana.fulltext.loader.service.MongoService;
import eu.europeana.fulltext.loader.service.XMLParserService;
import eu.europeana.fulltext.loader.web.LoaderController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Main application and configuration.
 *
 * @author Lúthien
 * Created on 27-02-2018
 */
@SpringBootApplication
@PropertySource(value = "classpath:build.properties", ignoreResourceNotFound = true)
@EnableMongoRepositories(basePackages="eu.europeana.fulltext")
public class LoaderApplication extends SpringBootServletInitializer {

	/**
	 * Load configuration from loader.properties file
	 */
	@Bean
	public LoaderSettings settings() { return new LoaderSettings(); }

	/**
	 * Service for parsing fulltext xml files
	 */
	@Bean
	public XMLParserService xmlParserService() {
		return new XMLParserService(settings());
	}

	/**
	 * Connection to mongo
	 */
	@Bean
	public MongoService mongoService() {
		return new MongoService();
	}


	/**
	 * Service that does the actual work, loading, parsing and sending data to Mongo
	 */
	@Bean
	public LoadArchiveService loadArchiveService() { return new LoadArchiveService(xmlParserService(), mongoService(), settings());}


	/**
	 * Rest controller that handles all requests
	 */
	@Bean
	public LoaderController loaderController() {
		return new LoaderController(loadArchiveService(), mongoService());
	}

	/**
	 * This method is called when starting as a Spring-Boot application (run this class from the IDE)
	 * @param args
	 */
	@SuppressWarnings("squid:S2095") // to avoid sonarqube false positive (see https://stackoverflow.com/a/37073154/741249)
	public static void main(String[] args)  {
		System.setProperty("logFileName", "application");

		// WARNING: we need to set a bigger entity expension limit because we have xml files with lots of entities.
		// For now we set it to 0 to disable any limit, but this makes us vulnerable to XML DDos attacks
		// See also https://stackoverflow.com/a/20482332
		System.setProperty("jdk.xml.entityExpansionLimit", "0");
		SpringApplication.run(LoaderApplication.class, args);
	}

}
