package gift;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class WishAcceptanceTest extends AcceptanceTest {
    @Test
    void 위시리스트에_상품을_추가하면_조회된다() {
        String token = registerAndGetToken("wish@example.com", "pass1234");
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("아이폰 16", 1350000, categoryId);

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("productId", productId))
        .when()
            .post("/api/wishes")
        .then()
            .statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/wishes")
        .then()
            .statusCode(200)
            .body("content", hasSize(1))
            .body("content[0].productId", equalTo(productId.intValue()));
    }

    @Test
    void 위시리스트에서_상품을_삭제하면_목록에서_제외된다() {
        String token = registerAndGetToken("wish@example.com", "pass1234");
        Long categoryId = createCategory("전자기기");
        Long productId1 = createProduct("아이폰 16", 1350000, categoryId);
        Long productId2 = createProduct("맥북 프로", 3360000, categoryId);

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("productId", productId1))
        .when()
            .post("/api/wishes")
        .then()
            .statusCode(201);

        Long wishId2 =
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("productId", productId2))
        .when()
            .post("/api/wishes")
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .delete("/api/wishes/{id}", wishId2)
        .then()
            .statusCode(204);

        given()
            .header("Authorization", "Bearer " + token)
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/wishes")
        .then()
            .statusCode(200)
            .body("content", hasSize(1))
            .body("content[0].productId", equalTo(productId1.intValue()));
    }

    @Test
    void 인증_없이_위시리스트에_접근하면_실패한다() {
        given()
        .when()
            .get("/api/wishes")
        .then()
            .statusCode(400);
    }
}
