package gift.restassured;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class ProductAcceptanceTest extends AcceptanceTest {
    @Test
    void 상품을_생성하면_목록에서_조회된다() {
        Long categoryId = createCategory("전자기기");

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "name", "아이폰 16",
                "price", 1350000,
                "imageUrl", "https://example.com/iphone.jpg",
                "categoryId", categoryId
            ))
        .when()
            .post("/api/products")
        .then()
            .statusCode(201);

        given()
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("content", hasSize(1))
            .body("content[0].name", equalTo("아이폰 16"));
    }

    @Test
    void 상품을_여러_개_생성하면_모두_목록에서_조회된다() {
        Long categoryId = createCategory("전자기기");

        createProduct("아이폰 16", 1350000, categoryId);
        createProduct("맥북 프로", 3360000, categoryId);
        createProduct("에어팟 프로", 359000, categoryId);

        given()
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("content", hasSize(3))
            .body("content.name", hasItems("아이폰 16", "맥북 프로", "에어팟 프로"));
    }

    @Test
    void 상품을_수정하면_변경된_내용이_조회된다() {
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("수정전", 5000, categoryId);

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "name", "수정후",
                "price", 9000,
                "imageUrl", "https://example.com/updated.jpg",
                "categoryId", categoryId
            ))
        .when()
            .put("/api/products/{id}", productId)
        .then()
            .statusCode(200);

        given()
        .when()
            .get("/api/products/{id}", productId)
        .then()
            .statusCode(200)
            .body("name", equalTo("수정후"))
            .body("price", equalTo(9000));
    }

    @Test
    void 상품을_삭제하면_목록에서_제외된다() {
        Long categoryId = createCategory("전자기기");
        createProduct("남는상품", 10000, categoryId);
        Long deleteId = createProduct("삭제용", 5000, categoryId);

        given()
        .when()
            .delete("/api/products/{id}", deleteId)
        .then()
            .statusCode(204);

        given()
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("content", hasSize(1))
            .body("content[0].name", equalTo("남는상품"));
    }
}
