package eu.europeana.fulltext.api;

import eu.europeana.fulltext.api.web.SocksProxyConfigInjector;
import org.apache.logging.log4j.LogManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;

/**
 * Main application and configuration.
 *
 * @author Lúthien
 * Created on 27-02-2018
 */
@EnableJpaRepositories(basePackages = "eu.europeana.fulltext.pgrepository")
@SpringBootApplication(scanBasePackages = {"eu.europeana.fulltext.api", "eu.europeana.fulltext.search", "eu.europeana.fulltext.repository"})
@PropertySource(value = "classpath:build.properties")
public class FTApplication extends SpringBootServletInitializer {

    public static final int THOUSAND = 1000;

    /**
     * This method is called when starting as a Spring-Boot application (run this class from the IDE)
     *
     * @param args
     */
    @SuppressWarnings("squid:S2095")
    // to avoid sonarqube false positive (see https://stackoverflow.com/a/37073154/741249)
    public static void main(String[] args) {
        LogManager.getLogger(FTApplication.class)
                  .info("CF_INSTANCE_INDEX  = {}, CF_INSTANCE_GUID = {}, CF_INSTANCE_IP  = {}",
                        System.getenv("CF_INSTANCE_INDEX"),
                        System.getenv("CF_INSTANCE_GUID"),
                        System.getenv("CF_INSTANCE_IP"));
        try {
            injectSocksProxySettings();
            SpringApplication.run(FTApplication.class, args);
        } catch (IOException e) {
            LogManager.getLogger(FTApplication.class).fatal("Error reading properties file", e);
            System.exit(-1);
        }
    }

    /**
     * This method is called when starting a 'traditional' war deployment (e.g. in Docker of Cloud Foundry)
     *
     * @param servletContext
     * @throws ServletException
     */
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        LogManager.getLogger(FTApplication.class)
                  .info("CF_INSTANCE_INDEX  = {}, CF_INSTANCE_GUID = {}, CF_INSTANCE_IP  = {}",
                        System.getenv("CF_INSTANCE_INDEX"),
                        System.getenv("CF_INSTANCE_GUID"),
                        System.getenv("CF_INSTANCE_IP"));
        try {
            injectSocksProxySettings();
            super.onStartup(servletContext);
        } catch (IOException e) {
            throw new ServletException("Error reading properties", e);
        }
    }

    /**
     * Socks proxy settings have to be loaded before anything else, so we check the property files for its settings
     *
     * @throws IOException if properties file cannot be read
     */
    private static void injectSocksProxySettings() throws IOException {
        SocksProxyConfigInjector socksConfig = new SocksProxyConfigInjector("fulltext.properties");
        try {
            socksConfig.addProperties("fulltext.user.properties");
        } catch (IOException e) {
            // user.properties may not be available so only show warning
            LogManager.getLogger(FTApplication.class).warn("Cannot read fulltext.user.properties file. Reason: ", e.getMessage());
        }
        socksConfig.inject();
    }

    /**
     * Configure CORS.
     * This would normally be done via WebMvcConfigurer.addCorsMapping(), but that doesn't set the
     * correct Access-Control-Allow-Origin:* header in the current Spring Boot version (2.1.7)
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(false);
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setAllowedOrigins(Collections.singletonList("*"));
        config.setAllowedMethods(Collections.singletonList("*"));
        config.setMaxAge(1000L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
