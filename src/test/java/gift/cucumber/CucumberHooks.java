package gift.cucumber;

import gift.support.DatabaseCleaner;
import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;

public class CucumberHooks {

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Before
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
        databaseCleaner.clear();
    }
}
