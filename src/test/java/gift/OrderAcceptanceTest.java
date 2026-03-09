package gift;

import io.restassured.http.ContentType;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

class OrderAcceptanceTest extends AcceptanceTestFixture {

    // --- 헬퍼 ---

    int getOptionQuantity(Long productId, Long optionId) {
        return given()
            .when()
            .get("/api/products/" + productId + "/options")
            .then()
            .statusCode(200)
            .extract().jsonPath().getInt("find { it.id == " + optionId.intValue() + " }.quantity");
    }

    long getOrderCount(String token) {
        return given()
            .header("Authorization", "Bearer " + token)
            .param("page", 0)
            .param("size", 100)
            .when()
            .get("/api/orders")
            .then()
            .statusCode(200)
            .extract().jsonPath().getLong("totalElements");
    }

    // --- GET /api/orders ---

    @Test
    void 주문_목록_조회_성공() {
        // given
        String token = registerAndGetToken("order@test.com", "pass");
        chargeMemberPoints("order@test.com", 100000);
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        Long optionId = createOption(productId, "8GB", 100);

        Long orderId = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("optionId", optionId, "quantity", 1, "message", "선물"))
            .when()
            .post("/api/orders")
            .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");

        // when
        var response = given()
            .header("Authorization", "Bearer " + token)
            .param("page", 0)
            .param("size", 10)
            .when()
            .get("/api/orders");

        // then
        response.then()
            .statusCode(200)
            .body("content.size()", is(1))
            .body("content[0].id", equalTo(orderId.intValue()))
            .body("content[0].optionId", equalTo(optionId.intValue()))
            .body("content[0].quantity", equalTo(1))
            .body("content[0].message", equalTo("선물"));
    }

    @Test
    void 주문_목록_조회_실패_인증헤더_누락() {
        // given
        // Authorization 헤더 없음

        // when
        var response = given()
            .param("page", 0)
            .param("size", 10)
            .when()
            .get("/api/orders");

        // then
        response.then()
            .statusCode(401);
    }

    // --- POST /api/orders ---

    @Test
    void 주문_생성_성공_재고차감_포인트차감() {
        // given
        String token = registerAndGetToken("buyer@test.com", "pass");
        chargeMemberPoints("buyer@test.com", 10000);
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        Long optionId = createOption(productId, "8GB", 50);
        int beforeQuantity = getOptionQuantity(productId, optionId);
        int beforePoint = getMemberPoint("buyer@test.com");
        long beforeOrderCount = getOrderCount(token);

        var request = Map.of(
            "optionId", optionId,
            "quantity", 3,
            "message", "생일 선물"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(request)
            .when()
            .post("/api/orders");

        // then
        response.then()
            .statusCode(201)
            .body("id", greaterThan(0))
            .body("optionId", equalTo(optionId.intValue()))
            .body("quantity", equalTo(3))
            .body("message", equalTo("생일 선물"))
            .body("orderDateTime", is(not(emptyString())));

        assertThat(getOptionQuantity(productId, optionId)).isEqualTo(beforeQuantity - 3);
        assertThat(getMemberPoint("buyer@test.com")).isEqualTo(beforePoint - 3000);
        assertThat(getOrderCount(token)).isEqualTo(beforeOrderCount + 1);
    }

    @Test
    void 주문_생성_실패_인증헤더_누락() {
        // given
        var request = Map.of("optionId", 1, "quantity", 1, "message", "test");

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/orders");

        // then
        response.then()
            .statusCode(401);
    }

    @Test
    void 주문_생성_실패_옵션ID_누락() {
        // given
        String token = registerAndGetToken("missing@test.com", "pass");
        var request = Map.of("quantity", 1, "message", "test");

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(request)
            .when()
            .post("/api/orders");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 주문_생성_실패_수량_0이하() {
        // given
        String token = registerAndGetToken("zero@test.com", "pass");
        var request = Map.of("optionId", 1, "quantity", 0, "message", "test");

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(request)
            .when()
            .post("/api/orders");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 주문_생성_실패_존재하지_않는_옵션() {
        // given
        String token = registerAndGetToken("nooption@test.com", "pass");
        var request = Map.of("optionId", nonExistingId(), "quantity", 1, "message", "test");

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(request)
            .when()
            .post("/api/orders");

        // then
        response.then()
            .statusCode(404);
    }

    @Test
    void 주문_생성_실패_재고_부족() {
        // given
        String token = registerAndGetToken("stock@test.com", "pass");
        chargeMemberPoints("stock@test.com", 100000);
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        Long optionId = createOption(productId, "8GB", 5);
        int beforeQuantity = getOptionQuantity(productId, optionId);
        int beforePoint = getMemberPoint("stock@test.com");
        long beforeOrderCount = getOrderCount(token);

        var request = Map.of("optionId", optionId, "quantity", 10, "message", "test");

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(request)
            .when()
            .post("/api/orders");

        // then
        response.then()
            .statusCode(400);

        assertThat(getOptionQuantity(productId, optionId)).isEqualTo(beforeQuantity);
        assertThat(getMemberPoint("stock@test.com")).isEqualTo(beforePoint);
        assertThat(getOrderCount(token)).isEqualTo(beforeOrderCount);
    }


    @Test
    void 주문_생성_성공_위시리스트_자동_삭제() {
        // given
        String token = registerAndGetToken("wish@test.com", "pass");
        chargeMemberPoints("wish@test.com", 100000);
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        Long optionId = createOption(productId, "8GB", 50);

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("productId", productId))
            .when()
            .post("/api/wishes")
            .then()
            .statusCode(201);

        long wishCountBefore = given()
            .header("Authorization", "Bearer " + token)
            .param("page", 0).param("size", 100)
            .when()
            .get("/api/wishes")
            .then()
            .statusCode(200)
            .extract().jsonPath().getLong("totalElements");
        assertThat(wishCountBefore).isEqualTo(1);

        // when
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(Map.of("optionId", optionId, "quantity", 1, "message", "선물"))
            .when()
            .post("/api/orders")
            .then()
            .statusCode(201);

        // then
        long wishCountAfter = given()
            .header("Authorization", "Bearer " + token)
            .param("page", 0).param("size", 100)
            .when()
            .get("/api/wishes")
            .then()
            .statusCode(200)
            .extract().jsonPath().getLong("totalElements");
        assertThat(wishCountAfter).isZero();
    }

    @Test
    void 주문_생성_실패_포인트_부족() {
        // given
        String token = registerAndGetToken("poor@test.com", "pass");
        // 포인트 0으로 시작 (충전하지 않음)
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1000, "http://img.test/1.png", categoryId);
        Long optionId = createOption(productId, "8GB", 50);
        int beforeQuantity = getOptionQuantity(productId, optionId);
        int beforePoint = getMemberPoint("poor@test.com");
        long beforeOrderCount = getOrderCount(token);

        var request = Map.of("optionId", optionId, "quantity", 1, "message", "test");

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(request)
            .when()
            .post("/api/orders");

        // then
        response.then()
            .statusCode(400);

        assertThat(getOptionQuantity(productId, optionId)).isEqualTo(beforeQuantity);
        assertThat(getMemberPoint("poor@test.com")).isEqualTo(beforePoint);
        assertThat(getOrderCount(token)).isEqualTo(beforeOrderCount);
    }
}
