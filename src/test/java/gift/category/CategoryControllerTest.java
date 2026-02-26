package gift.category;

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
class CategoryControllerTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("GET /api/categories - 카테고리 목록을 조회한다")
    void getCategories() {
        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body("[0].name", notNullValue());
    }

    @Test
    @DisplayName("POST /api/categories - 카테고리를 생성하면 201을 반환한다")
    void createCategory() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "새 카테고리",
                "color", "#FF0000",
                "imageUrl", "http://img.test/c.png",
                "description", "설명"
            ))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(201)
            .body("name", equalTo("새 카테고리"));
    }

    @Test
    @DisplayName("PUT /api/categories/{id} - 카테고리를 수정하면 200을 반환한다")
    void updateCategory() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "수정됨",
                "color", "#FF0000",
                "imageUrl", "http://img.test/c2.png",
                "description", "수정 설명"
            ))
        .when()
            .put("/api/categories/1")
        .then()
            .statusCode(200)
            .body("name", equalTo("수정됨"));
    }

    @Test
    @DisplayName("PUT /api/categories/{id} - 존재하지 않는 ID면 404를 반환한다")
    void updateCategoryNotFound() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "수정",
                "color", "#FF0000",
                "imageUrl", "http://img.test/c.png",
                "description", "설명"
            ))
        .when()
            .put("/api/categories/99999")
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} - 카테고리를 삭제하면 204를 반환한다")
    void deleteCategory() {
        // 상품이 없는 카테고리를 생성 후 삭제
        int newId = given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "삭제용",
                "color", "#000000",
                "imageUrl", "http://img.test/c.png",
                "description", "삭제"
            ))
        .when()
            .post("/api/categories")
        .then()
            .extract().path("id");

        given()
        .when()
            .delete("/api/categories/" + newId)
        .then()
            .statusCode(204);
    }
}
