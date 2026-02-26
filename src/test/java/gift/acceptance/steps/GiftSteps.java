package gift.acceptance.steps;

import gift.acceptance.TestContext;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.먼저;
import io.cucumber.java.ko.만일;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.hamcrest.Matchers.equalTo;

public class GiftSteps {

    @Autowired
    private TestContext testContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Response lastGiftResponse;

    @먼저("재고가 {int}인 옵션을 생성한다")
    public void 옵션을_생성한다(int quantity) {
        Long optionId = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {"name": "블랙", "quantity": %d}
                        """, quantity))
                .post("/api/products/" + testContext.getProductId() + "/options")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
        testContext.setOptionId(optionId);
    }

    @먼저("보내는 회원을 생성한다")
    public void 보내는_회원을_생성한다() {
        String token = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "hong@example.com", "password": "password123"}
                        """)
                .post("/api/members/register")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("token");
        testContext.setSenderToken(token);

        jdbcTemplate.update(
                "UPDATE member SET point = ? WHERE email = ?",
                100_000_000, "hong@example.com"
        );
    }

    @먼저("받는 회원을 생성한다")
    public void 받는_회원을_생성한다() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "kim@example.com", "password": "password123"}
                        """)
                .post("/api/members/register")
                .then()
                .statusCode(201);
    }

    @만일("{int}개의 선물을 보낸다")
    public void N개의_선물을_보낸다(int quantity) {
        lastGiftResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + testContext.getSenderToken())
                .body(String.format("""
                        {"optionId": %d, "quantity": %d, "message": "선물이에요"}
                        """, testContext.getOptionId(), quantity))
                .when()
                .post("/api/orders");
    }

    @그러면("응답 상태코드가 {int}이다")
    public void 응답_상태코드_확인(int expectedStatusCode) {
        lastGiftResponse.then()
                .statusCode(expectedStatusCode);
    }

    @그리고("옵션의 재고가 {int}이다")
    public void 옵션의_재고_확인(int expectedQuantity) {
        RestAssured.given()
                .when()
                .get("/api/products/" + testContext.getProductId() + "/options")
                .then()
                .statusCode(200)
                .body("find { it.id == " + testContext.getOptionId() + " }.quantity", equalTo(expectedQuantity));
    }
}
