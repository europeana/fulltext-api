package eu.europeana.fulltext.api;

import eu.europeana.fulltext.api.web.SocksProxyConfigInjector;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main application and configuration.
 *
 * @author Lúthien
 * Created on 27-02-2018
 */
@SpringBootApplication(scanBasePackages = {"eu.europeana.fulltext.api", "eu.europeana.fulltext.repository"})
@PropertySource(value = "classpath:build.properties")
@PropertySource("classpath:fulltext.properties")
@PropertySource(value = "classpath:fulltext.user.properties", ignoreResourceNotFound = true)
public class FTApplication extends SpringBootServletInitializer {

    public static final int THOUSAND = 1000;

    @Value("${security.config.ipRanges}")
    private String ipRanges;

    /**
     * Setup CORS for all requests
     *
     * @return
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebConfig();
    }

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
            LogManager.getLogger(FTApplication.class).warn("Cannot read fulltext.user.properties file", e);
        }
        socksConfig.inject();
    }

    @Configuration
    static class WebConfig implements WebMvcConfigurer {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**").allowedOrigins("*").maxAge(THOUSAND);
        }
    }

    @EnableWebSecurity
    @Configuration
    class SecurityConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            if(StringUtils.isNotEmpty(ipRanges)) {
                http.authorizeRequests()
                        .antMatchers("/**").access(createHasIpRangeExpression());
            }
        }

        /**
         * creates the string for authorizing request for the provided ipRanges
         */
        private String createHasIpRangeExpression() {
            List<String> validIps = Arrays.asList(ipRanges.split("\\s*,\\s*"));
            return validIps.stream()
                    .collect(Collectors.joining("') or hasIpAddress('", "hasIpAddress('", "')"));
        }
    }

}
