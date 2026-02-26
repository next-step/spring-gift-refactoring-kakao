package gift;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

class OrderAcceptanceTest extends AcceptanceTest {
    @Test
    void 주문을_생성하면_주문_목록에서_조회된다() {
        String token = registerAndGetToken("order@example.com", "pass1234");
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("아이폰 16", 1350000, categoryId);
        Long optionId = createOption(productId, "블루 256GB", 30);

        given()
            .param("amount", 10000000)
        .when()
            .post("/admin/members/{id}/charge-point", 1)
        .then()
            .statusCode(302);

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header("Authorization", "Bearer " + token)
            .body(Map.of(
                "optionId", optionId,
                "quantity", 1,
                "message", "선물입니다"
            ))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/orders")
        .then()
            .statusCode(200)
            .body("content", hasSize(1))
            .body("content[0].quantity", equalTo(1))
            .body("content[0].message", equalTo("선물입니다"));
    }

    @Test
    void 인증_없이_주문에_접근하면_실패한다() {
        given()
        .when()
            .get("/api/orders")
        .then()
            .statusCode(400);
    }
}
