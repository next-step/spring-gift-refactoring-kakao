package gift.cucumber.steps;

import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class CommonStepDefinitions {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 28080;
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE orders");
        jdbcTemplate.execute("TRUNCATE TABLE wish");
        jdbcTemplate.execute("TRUNCATE TABLE options");
        jdbcTemplate.execute("TRUNCATE TABLE product");
        jdbcTemplate.execute("TRUNCATE TABLE category");
        jdbcTemplate.execute("TRUNCATE TABLE member");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
