package gift;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

class OptionAcceptanceTest extends AcceptanceTest {
    @Test
    void 옵션을_추가하면_상품의_옵션_목록에서_조회된다() {
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("아이폰 16", 1350000, categoryId);

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of(
                "name", "블루 256GB",
                "quantity", 30)
            )
        .when()
            .post("/api/products/{productId}/options", productId)
        .then()
            .statusCode(201);

        given()
        .when()
            .get("/api/products/{productId}/options", productId)
        .then()
            .statusCode(200)
            .body(".", hasSize(1))
            .body("[0].name", equalTo("블루 256GB"));
    }

    @Test
    void 옵션을_여러_개_추가하면_모두_조회된다() {
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("아이폰 16", 1350000, categoryId);

        createOption(productId, "블루 256GB", 30);
        createOption(productId, "블랙 512GB", 20);
        createOption(productId, "실버 128GB", 50);

        given()
        .when()
            .get("/api/products/{productId}/options", productId)
        .then()
            .statusCode(200)
            .body(".", hasSize(3))
            .body("name", hasItems("블루 256GB", "블랙 512GB", "실버 128GB"));
    }

    @Test
    void 옵션이_2개일_때_하나를_삭제하면_1개만_남는다() {
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("아이폰 16", 1350000, categoryId);
        createOption(productId, "블루 256GB", 30);
        Long deleteId = createOption(productId, "블랙 512GB", 20);

        given()
        .when()
            .delete("/api/products/{productId}/options/{optionId}", productId, deleteId)
        .then()
            .statusCode(204);

        given()
        .when()
            .get("/api/products/{productId}/options", productId)
        .then()
            .statusCode(200)
            .body(".", hasSize(1))
            .body("[0].name", equalTo("블루 256GB"));
    }

    @Test
    void 마지막_옵션은_삭제할_수_없다() {
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("아이폰 16", 1350000, categoryId);
        Long optionId = createOption(productId, "블루 256GB", 30);

        given()
        .when()
            .delete("/api/products/{productId}/options/{optionId}", productId, optionId)
        .then()
            .statusCode(400);
    }
}
