package gift.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "spring.flyway.target=1")
public class CucumberSpringConfig {

    @TestConfiguration
    static class Config {
        @Bean
        @Scope("cucumber-glue")
        public SharedState sharedState() {
            return new SharedState();
        }
    }

    @LocalServerPort
    int port;

    @Autowired
    DataSource dataSource;

    @io.cucumber.java.Before
    public void setUp() {
        RestAssured.port = port;
        cleanDatabase();
    }

    private void cleanDatabase() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbc.queryForList(
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
            "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'"
        ).forEach(row -> {
            String tableName = row.get("TABLE_NAME").toString();
            if (!"flyway_schema_history".equalsIgnoreCase(tableName)) {
                jdbc.execute("TRUNCATE TABLE " + tableName + " RESTART IDENTITY");
            }
        });
        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
