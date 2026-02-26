package gift;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class CategoryAcceptanceTest extends AcceptanceTest {
    @Test
    void 카테고리를_생성하면_목록에서_조회된다() {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "name", "음료",
                "color", "#1E90FF",
                "imageUrl", "https://example.com/drink.jpg",
                "description", "음료 카테고리"
            ))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(201);

        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body(".", hasSize(1)) // root
            .body("[0].name", equalTo("음료"));
    }

    @Test
    void 카테고리를_여러_개_생성하면_모두_목록에서_조회된다() {
        createCategory("음료");
        createCategory("디저트");
        createCategory("케이크");

        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body(".", hasSize(3))
            .body("name", hasItems("음료", "디저트", "케이크"));
    }

    @Test
    void 카테고리를_수정하면_변경된_내용이_조회된다() {
        Long id = createCategory("수정전");

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "name", "수정후",
                "color", "#FFFFFF",
                "imageUrl", "https://example.com/updated.jpg",
                "description", "수정된 설명"
            ))
        .when()
            .put("/api/categories/{id}", id)
        .then()
            .statusCode(200);

        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body(".", hasSize(1))
            .body("[0].name", equalTo("수정후"));
    }

    @Test
    void 카테고리를_삭제하면_목록에서_제외된다() {
        createCategory("남는것");
        Long deleteId = createCategory("삭제용");

        given()
        .when()
            .delete("/api/categories/{id}", deleteId)
        .then()
            .statusCode(204);

        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body(".", hasSize(1))
            .body("[0].name", equalTo("남는것"));
    }
}
