package gift.product;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/setup-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AdminProductControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("관리자는 카카오가 포함된 상품명으로 상품을 생성할 수 있다")
    void createProductWithKakaoName() {
        given()
            .contentType("application/x-www-form-urlencoded; charset=UTF-8")
            .formParam("name", "카카오선물")
            .formParam("price", 1000)
            .formParam("imageUrl", "http://img.test/p.png")
            .formParam("categoryId", 1)
            .redirects().follow(false)
        .when()
            .post("/admin/products")
        .then()
            .statusCode(302);

        assertTrue(productRepository.findAll().stream()
            .anyMatch(p -> p.getName().equals("카카오선물")));
    }
}
