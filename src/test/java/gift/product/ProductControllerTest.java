package gift.product;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/setup-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ProductControllerTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("GET /api/products - 상품 목록을 페이지로 조회한다")
    void getProducts() {
        given()
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("content", notNullValue());
    }

    @Test
    @DisplayName("GET /api/products/{id} - 상품을 조회하면 200을 반환한다")
    void getProduct() {
        given()
        .when()
            .get("/api/products/1")
        .then()
            .statusCode(200)
            .body("name", equalTo("테스트 상품"));
    }

    @Test
    @DisplayName("GET /api/products/{id} - 존재하지 않는 ID면 404를 반환한다")
    void getProductNotFound() {
        given()
        .when()
            .get("/api/products/99999")
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("POST /api/products - 상품을 생성하면 201을 반환한다")
    void createProduct() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "새 상품",
                "price", 2000,
                "imageUrl", "http://img.test/p.png",
                "categoryId", 1
            ))
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .body("name", equalTo("새 상품"));
    }

    @Test
    @DisplayName("POST /api/products - 이름이 15자를 초과하면 400을 반환한다")
    void createProductInvalidName() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "a".repeat(16),
                "price", 2000,
                "imageUrl", "http://img.test/p.png",
                "categoryId", 1
            ))
        .when()
            .post("/api/products")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("PUT /api/products/{id} - 상품을 수정하면 200을 반환한다")
    void updateProduct() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "수정 상품",
                "price", 3000,
                "imageUrl", "http://img.test/p2.png",
                "categoryId", 1
            ))
        .when()
            .put("/api/products/1")
        .then()
            .statusCode(200)
            .body("name", equalTo("수정 상품"));
    }

    @Test
    @DisplayName("DELETE /api/products/{id} - 상품을 삭제하면 204를 반환한다")
    void deleteProduct() {
        // FK 제약 없는 상품을 생성 후 삭제
        int newId = given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "삭제용",
                "price", 1000,
                "imageUrl", "http://img.test/p.png",
                "categoryId", 1
            ))
        .when()
            .post("/api/products")
        .then()
            .extract().path("id");

        given()
        .when()
            .delete("/api/products/" + newId)
        .then()
            .statusCode(204);
    }
}
