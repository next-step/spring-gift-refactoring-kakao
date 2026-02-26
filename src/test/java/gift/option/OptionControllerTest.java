package gift.option;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/setup-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class OptionControllerTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("GET /api/products/{productId}/options - 옵션 목록을 조회한다")
    void getOptions() {
        given()
        .when()
            .get("/api/products/1/options")
        .then()
            .statusCode(200)
            .body("[0].name", equalTo("기본 옵션"));
    }

    @Test
    @DisplayName("GET /api/products/{productId}/options - 존재하지 않는 상품이면 404를 반환한다")
    void getOptionsProductNotFound() {
        given()
        .when()
            .get("/api/products/99999/options")
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("POST /api/products/{productId}/options - 옵션을 생성하면 201을 반환한다")
    void createOption() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "새 옵션", "quantity", 5))
        .when()
            .post("/api/products/1/options")
        .then()
            .statusCode(201)
            .body("name", equalTo("새 옵션"));
    }

    @Test
    @DisplayName("POST /api/products/{productId}/options - 중복 옵션명이면 400을 반환한다")
    void createOptionDuplicateName() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "기본 옵션", "quantity", 5))
        .when()
            .post("/api/products/1/options")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("DELETE - 옵션이 2개 이상이면 삭제 후 204를 반환한다")
    void deleteOption() {
        // setup-data.sql에 옵션 2개 (id=1, id=2) 존재
        given()
        .when()
            .delete("/api/products/1/options/2")
        .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("DELETE - 마지막 옵션이면 400을 반환한다")
    void deleteLastOption() {
        // 먼저 옵션 하나 삭제하여 1개만 남김
        given().delete("/api/products/1/options/2");

        given()
        .when()
            .delete("/api/products/1/options/1")
        .then()
            .statusCode(400);
    }
}
