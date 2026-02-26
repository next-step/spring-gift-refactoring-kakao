package gift;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = "spring.flyway.target=1")
public abstract class AcceptanceTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    protected String registerAndGetToken(String email, String password) {
        return given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "email", email,
                "password", password
            ))
        .when()
            .post("/api/members/register")
        .then()
            .statusCode(201)
            .extract().jsonPath().getString("token");
    }

    protected Long createCategory(String name) {
        return given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "name", name,
                "color", "#000000",
                "imageUrl", "https://example.com/image.jpg",
                "description", "test"
            ))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
    }

    protected Long createProduct(String name, int price, Long categoryId) {
        return given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "name", name,
                "price", price,
                "imageUrl", "https://example.com/image.jpg",
                "categoryId", categoryId
            ))
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
    }

    protected Long createOption(Long productId, String name, int quantity) {
        return given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "name", name,
                "quantity", quantity
            ))
        .when()
            .post("/api/products/{productId}/options", productId)
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
    }
}
