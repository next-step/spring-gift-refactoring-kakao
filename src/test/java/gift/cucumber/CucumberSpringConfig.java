package gift.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "spring.flyway.target=1")
public class CucumberSpringConfig {

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void setUp() {
        RestAssured.port = port;
    }

    @io.cucumber.java.Before
    public void cleanDatabase() {
        new ResourceDatabasePopulator(new ClassPathResource("cleanup.sql"))
            .execute(dataSource);
    }
}
