package gift.acceptance;

import gift.support.DataManipulator;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.springframework.boot.test.web.server.LocalServerPort;

public class CucumberHooks {

    private final DataManipulator dataManipulator;

    private final int port;

    public CucumberHooks(
            DataManipulator dataManipulator,
            @LocalServerPort int port
    ) {
        this.dataManipulator = dataManipulator;
        this.port = port;
    }

    @Before(order = 0)
    public void setUp() {
        RestAssured.reset();
        RestAssured.port = port;
        RestAssured.filters(
                new RequestLoggingFilter(),
                new ResponseLoggingFilter()
        );
    }

    @After(order = 0)
    public void cleanUp() {
        dataManipulator.initAll();
    }
}
