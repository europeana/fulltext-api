package eu.europeana.fulltext.loader;

import eu.europeana.fulltext.loader.config.LoaderSettings;
import eu.europeana.fulltext.loader.service.LoadArchiveService;
import eu.europeana.fulltext.loader.service.MongoService;
import eu.europeana.fulltext.loader.web.LoaderController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

/**
 * Main application and configuration.
 *
 * @author Lúthien
 * Created on 27-02-2018
 */
@SpringBootApplication
@PropertySource(value = "classpath:build.properties", ignoreResourceNotFound = true)
public class LoaderApplication extends SpringBootServletInitializer {

	/**
	 * Load configuration from loader.properties file
	 * @return
	 */
	@Bean
	public LoaderSettings settings() { return new LoaderSettings(); }

	/**
	 * Connection to mongo
	 * @return
	 */
	@Bean
	public MongoService mongoService() {
		return new MongoService();
	}


	/**
	 * Service that does the actual work, loading, parsing and sending data to Mongo
	 */
	@Bean
	public LoadArchiveService loadArchiveService() { return new LoadArchiveService(mongoService(), settings());}


	/**
	 * Rest controller that handles all requests
	 * @return
	 */
	@Bean
	public LoaderController loaderController() {
		return new LoaderController(loadArchiveService());
	}

	/**
	 * This method is called when starting as a Spring-Boot application (run this class from the IDE)
	 * @param args
	 */
	@SuppressWarnings("squid:S2095") // to avoid sonarqube false positive (see https://stackoverflow.com/a/37073154/741249)
	public static void main(String[] args)  {
		System.setProperty("logFileName", "application");
		SpringApplication.run(LoaderApplication.class, args);
	}

}
