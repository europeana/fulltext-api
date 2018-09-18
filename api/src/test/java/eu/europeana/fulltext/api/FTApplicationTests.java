package eu.europeana.fulltext.api;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.platform.runner.JUnitPlatform;

@Disabled
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class FTApplicationTests {

	/**
	 * Basic Spring-Boot context load test
	 */
	// TODO fix so this will load with a mock database

	@Test
	public void contextLoads() {
	}

}
